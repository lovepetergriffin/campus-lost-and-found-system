package com.campus.lostfound.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录令牌的签发与校验服务。
 *
 * <p>令牌采用 JWS 紧凑序列化格式：
 * {@code base64url(header) . base64url(payload) . base64url(signature)}，
 * 签名算法 HS256（HMAC-SHA256），载荷含 {@code sub}（用户 id）、{@code username}、
 * {@code role}、{@code exp}（过期时间戳，单位秒）。
 *
 * <p>本项目未引入第三方 JWT 库，此处按 RFC 7515 的最小可用子集手写实现，
 * 只服务"签名可信 + 未过期"两项校验。密钥来自配置项 {@code app.security.token-secret}，
 * 生产部署必须通过环境变量覆盖默认值（见 README 的安全声明）。
 *
 * <p>本类<b>无状态</b>，不持有会话；签发后服务端不留存令牌，登出的语义是"客户端丢弃令牌"。
 */
@Service
public class AuthTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenService.class);

    /** JWS 紧凑序列化的段分隔符。 */
    private static final String SEGMENT_SEPARATOR = ".";

    /** JWS 紧凑序列化的段数：header / payload / signature。 */
    private static final int SEGMENT_COUNT = 3;

    /** JCE 中的 HMAC 算法名（注意与 JOSE 头里的 {@code HS256} 写法不同，指同一算法）。 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public AuthTokenService(ObjectMapper objectMapper,
                            @Value("${app.security.token-secret}") String secret,
                            @Value("${app.security.token-expiration-seconds:604800}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * 为已认证用户签发令牌。
     *
     * @param principal 用户主体，其 id / username / role 会写进载荷
     * @return 紧凑序列化的令牌字符串
     * @throws IllegalStateException 载荷序列化或签名运算失败时抛出（属服务端故障，非用户输入问题）
     */
    public String create(UserPrincipal principal) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", principal.id());
            payload.put("username", principal.username());
            payload.put("role", principal.role());
            payload.put("exp", Instant.now().getEpochSecond() + expirationSeconds);
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String unsigned = header + SEGMENT_SEPARATOR + body;
            return unsigned + SEGMENT_SEPARATOR + encode(sign(unsigned));
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成登录令牌", ex);
        }
    }

    /**
     * 解析并校验令牌。
     *
     * <p>所有失败情形 —— 段数不符、Base64 非法、JSON 畸形、签名不匹配、已过期 ——
     * 都统一返回 {@code null}，由调用方按"匿名请求"处理。这让调用侧无需区分失败原因，
     * 也避免向客户端泄漏"令牌到底哪一项不对"。
     *
     * <p>失败原因会记入 debug 日志：线上排查"用户突然掉线"时，靠它区分是过期还是签名不符。
     *
     * @param token 待校验的令牌原文
     * @return 校验通过时返回用户主体，否则返回 {@code null}
     */
    public UserPrincipal parse(String token) {
        try {
            return parseChecked(token);
        } catch (Exception ex) {
            LOGGER.debug("令牌校验未通过，按匿名请求处理：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 执行真正的解析流程：切段 → 验签 → 解载荷 → 验声明。
     *
     * <p><b>顺序不可调换</b>：必须先验签再解 JSON，否则未通过签名校验的载荷会先进入
     * JSON 反序列化，白白给攻击者一个构造畸形数据的入口。
     *
     * @throws Exception 任一环节不通过时抛出，由 {@link #parse(String)} 统一兜底
     */
    private UserPrincipal parseChecked(String token) throws Exception {
        String[] parts = splitIntoThreeSegments(token);
        verifySignature(parts);
        Map<String, Object> payload = readPayload(parts[1]);
        validateClaims(payload);
        return toPrincipal(payload);
    }

    /**
     * 按 {@link #SEGMENT_SEPARATOR} 切分令牌并校验段数。
     *
     * @throws IllegalArgumentException 段数不等于 3 时抛出
     */
    private String[] splitIntoThreeSegments(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != SEGMENT_COUNT) {
            throw new IllegalArgumentException("令牌段数非法");
        }
        return parts;
    }

    /**
     * 强制解引用 {@code exp} 字段，校验令牌是否仍在有效期内。
     *
     * @param payload 已解码的载荷
     * @throws SecurityException 令牌已过期时抛出
     */
    private void validateClaims(Map<String, Object> payload) {
        long expiresAt = ((Number) payload.get("exp")).longValue();
        if (expiresAt < Instant.now().getEpochSecond()) {
            throw new SecurityException("令牌已过期");
        }
    }

    /**
     * 用密钥重算签名并与令牌自带签名比对。
     *
     * <p>用 {@link MessageDigest#isEqual} 而非 {@code Arrays.equals}：前者是定长比较，
     * 不会因为"前几个字节就对不上"而提前返回，从而不泄漏可用于逐字节爆破的时间差。
     *
     * @throws SecurityException 签名不匹配时抛出
     */
    private void verifySignature(String[] parts) throws Exception {
        byte[] expected = sign(parts[0] + SEGMENT_SEPARATOR + parts[1]);
        if (!MessageDigest.isEqual(expected, DECODER.decode(parts[2]))) {
            throw new SecurityException("令牌签名不匹配");
        }
    }

    /**
     * 把 base64url 载荷段解码并反序列化为键值表。
     *
     * @throws Exception Base64 非法或 JSON 畸形时抛出
     */
    private Map<String, Object> readPayload(String payloadSegment) throws Exception {
        return objectMapper.readValue(DECODER.decode(payloadSegment), new TypeReference<>() {});
    }

    /**
     * 把载荷映射为用户主体。调用前必须已通过 {@link #verifySignature}，
     * 因此这里的强制类型转换可以信任。
     */
    private UserPrincipal toPrincipal(Map<String, Object> payload) {
        return new UserPrincipal(
                ((Number) payload.get("sub")).longValue(),
                String.valueOf(payload.get("username")),
                String.valueOf(payload.get("role"))
        );
    }

    /**
     * 用 HMAC-SHA256 对给定内容签名。
     *
     * <p>每次调用都新建 {@link Mac} 实例而不复用：{@code Mac} 本身不是线程安全的，
     * 而本方法会被并发请求同时进入。新建的开销远低于加锁，这里是刻意的取舍。
     *
     * @param value 待签名的原文（签发时是 {@code header.payload}，校验时同）
     * @return HMAC 摘要的原始字节，交由 {@link #encode(byte[])} 做 base64url 编码
     * @throws Exception 算法不可用或密钥非法时抛出（属部署配置问题）
     */
    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 把字节串编码为 JWS 要求的 base64url 文本。
     *
     * <p>用 URL 安全字母表（{@code -_} 替代 {@code +/}）是为了让令牌能原样放进
     * 请求头与 URL；去掉 {@code =} 填充则是 JWS 紧凑序列化的硬性要求 ——
     * 保留填充会让签名段末尾出现 {@code =}，与段分隔符的解析相互干扰。
     *
     * @param value 待编码字节
     * @return 不含填充的 base64url 字符串
     */
    private String encode(byte[] value) {
        return ENCODER.encodeToString(value);
    }
}
