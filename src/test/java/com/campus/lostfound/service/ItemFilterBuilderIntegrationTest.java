package com.campus.lostfound.service;

import com.campus.lostfound.domain.Item;
import com.campus.lostfound.mapper.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemFilterBuilderIntegrationTest {
    @Autowired ItemFilterBuilder filterBuilder;
    @Autowired ItemMapper itemMapper;
    @Autowired JdbcTemplate jdbc;

    private Long categoryId;
    private Long otherCategoryId;
    private Long publisherId;
    private static final LocalDate DAY = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        // 回滚隔离测试数据，不依赖其他测试是否已创建物品或分类。
        jdbc.update("DELETE FROM review_record");
        jdbc.update("DELETE FROM claim");
        jdbc.update("DELETE FROM item");
        jdbc.update("INSERT INTO category (name, sort_order) VALUES (?, ?)", "筛选测试分类A", 900);
        jdbc.update("INSERT INTO category (name, sort_order) VALUES (?, ?)", "筛选测试分类B", 901);
        categoryId = jdbc.queryForObject("SELECT id FROM category WHERE name = ?", Long.class, "筛选测试分类A");
        otherCategoryId = jdbc.queryForObject("SELECT id FROM category WHERE name = ?", Long.class, "筛选测试分类B");
        publisherId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, "admin");
    }

    @Test
    void onlyPublishedItemsAreListedNewestFirst() {
        Item older = insert("旧物品", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay());
        Item newer = insert("新物品", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atTime(12, 0));
        for (String status : List.of("PENDING", "REJECTED", "CLAIMED", "CLOSED")) {
            insert(status, "普通描述", "FOUND", categoryId, "图书馆", status, DAY.atTime(13, 0));
        }
        assertThat(find(null, null, null, null, null, null)).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void blankOptionalTextDoesNotFilter() {
        Item item = insert("校园卡", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay());
        assertThat(find(" \t", " ", null, "\n", null, null)).containsExactly(item.getId());
    }

    @Test
    void keywordMatchesNameOrDescriptionWithoutLeakingUnpublishedItems() {
        Item nameMatch = insert("蓝色校园卡", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay());
        Item descriptionMatch = insert("卡套", "内有校园卡", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atTime(1, 0));
        insert("钥匙", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atTime(2, 0));
        insert("隐藏卡套", "内有校园卡", "FOUND", categoryId, "图书馆", "PENDING", DAY.atTime(3, 0));
        assertThat(find(" 校园卡 ", null, null, null, null, null))
                .containsExactly(descriptionMatch.getId(), nameMatch.getId());
    }

    @Test
    void optionalFiltersWorkIndividuallyAndTogether() {
        Item target = insert("校园卡", "普通描述", "FOUND", categoryId, "图书馆二楼", "PUBLISHED", DAY.atStartOfDay());
        Item lost = insert("校园卡", "普通描述", "LOST", categoryId, "图书馆二楼", "PUBLISHED", DAY.atTime(1, 0));
        Item otherCategory = insert("校园卡", "普通描述", "FOUND", otherCategoryId, "图书馆二楼", "PUBLISHED", DAY.atTime(2, 0));
        Item otherLocation = insert("校园卡", "普通描述", "FOUND", categoryId, "食堂", "PUBLISHED", DAY.atTime(3, 0));
        assertThat(find(null, "found", null, null, null, null))
                .containsExactly(otherLocation.getId(), otherCategory.getId(), target.getId());
        assertThat(find(null, null, categoryId, null, null, null))
                .containsExactly(otherLocation.getId(), lost.getId(), target.getId());
        assertThat(find(null, null, null, " 图书馆 ", null, null))
                .containsExactly(otherCategory.getId(), lost.getId(), target.getId());
        assertThat(find("校园卡", "found", categoryId, " 图书馆 ", DAY, DAY)).containsExactly(target.getId());
    }

    @Test
    void dateRangeIncludesEntireEndDayButExcludesFollowingMidnight() {
        insert("前一天", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay().minusSeconds(1));
        Item start = insert("开始", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay());
        Item end = insert("结束", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atTime(23, 59, 59));
        insert("后一天", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.plusDays(1).atStartOfDay());
        assertThat(find(null, null, null, null, DAY, DAY)).containsExactly(end.getId(), start.getId());
        assertThat(find(null, null, null, null, DAY.plusDays(1), DAY)).isEmpty();
    }

    @Test
    void dateRangeAllowsEitherBoundToBeAbsent() {
        Item before = insert("前一天", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.minusDays(1).atStartOfDay());
        Item today = insert("当天", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.atStartOfDay());
        Item after = insert("后一天", "普通描述", "FOUND", categoryId, "图书馆", "PUBLISHED", DAY.plusDays(1).atStartOfDay());
        assertThat(find(null, null, null, null, DAY, null)).containsExactly(after.getId(), today.getId());
        assertThat(find(null, null, null, null, null, DAY)).containsExactly(today.getId(), before.getId());
    }

    private List<Long> find(String keyword, String type, Long category, String location,
                            LocalDate start, LocalDate end) {
        return itemMapper.selectList(filterBuilder.build(keyword, type, category, location, start, end))
                .stream().map(Item::getId).toList();
    }

    private Item insert(String name, String description, String type, Long category, String location,
                        String status, LocalDateTime eventTime) {
        Item item = new Item();
        item.setName(name);
        item.setDescription(description);
        item.setType(type);
        item.setCategoryId(category);
        item.setLocation(location);
        item.setStatus(status);
        item.setEventTime(eventTime);
        item.setUserId(publisherId);
        item.setCreatedAt(eventTime);
        item.setUpdatedAt(eventTime);
        itemMapper.insert(item);
        return item;
    }
}
