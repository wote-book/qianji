package com.example.finance.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预设分类：不建表，注册时复制为当前用户自己的分类记录。
 */
public final class PresetCategories {

    public record Preset(String name, String type, String icon) {
    }

    public static final List<Preset> ALL = List.of(
            new Preset("餐饮", "EXPENSE", "bowl"),
            new Preset("交通", "EXPENSE", "bus"),
            new Preset("购物", "EXPENSE", "shopping-bag"),
            new Preset("居住", "EXPENSE", "house"),
            new Preset("娱乐", "EXPENSE", "game-controller"),
            new Preset("医疗", "EXPENSE", "first-aid-kit"),
            new Preset("教育", "EXPENSE", "reading"),
            new Preset("其他支出", "EXPENSE", "more"),
            new Preset("工资", "INCOME", "money"),
            new Preset("奖金", "INCOME", "trophy"),
            new Preset("理财收益", "INCOME", "trend-charts"),
            new Preset("其他收入", "INCOME", "more")
    );

    /** 关键词 -> 预设分类名，用于 CSV 自动分类 */
    public static final Map<String, String> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("外卖", "餐饮");
        KEYWORDS.put("餐", "餐饮");
        KEYWORDS.put("饭", "餐饮");
        KEYWORDS.put("咖啡", "餐饮");
        KEYWORDS.put("奶茶", "餐饮");
        KEYWORDS.put("超市", "购物");
        KEYWORDS.put("淘宝", "购物");
        KEYWORDS.put("京东", "购物");
        KEYWORDS.put("地铁", "交通");
        KEYWORDS.put("公交", "交通");
        KEYWORDS.put("打车", "交通");
        KEYWORDS.put("滴滴", "交通");
        KEYWORDS.put("加油", "交通");
        KEYWORDS.put("房租", "居住");
        KEYWORDS.put("水电", "居住");
        KEYWORDS.put("物业", "居住");
        KEYWORDS.put("电影", "娱乐");
        KEYWORDS.put("游戏", "娱乐");
        KEYWORDS.put("健身", "娱乐");
        KEYWORDS.put("医院", "医疗");
        KEYWORDS.put("药", "医疗");
        KEYWORDS.put("学费", "教育");
        KEYWORDS.put("书店", "教育");
        KEYWORDS.put("课程", "教育");
        KEYWORDS.put("工资", "工资");
        KEYWORDS.put("奖金", "奖金");
        KEYWORDS.put("利息", "理财收益");
    }

    private PresetCategories() {
    }
}
