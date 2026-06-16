package com.fund.service;

import com.fund.entity.UserWatchlist;
import com.fund.entity.WatchlistGroup;
import com.fund.mapper.UserWatchlistMapper;
import com.fund.mapper.WatchlistGroupMapper;
import com.fund.vo.FundData;
import com.fund.service.FundDataService;
import com.fund.vo.WatchlistCompareVO;
import com.fund.vo.WatchlistGroupVO;
import com.fund.vo.WatchlistItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WatchlistService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistService.class);
    private static final int MAX_WATCHLIST_SIZE = 200;

    @Resource
    private UserWatchlistMapper userWatchlistMapper;

    @Resource
    private WatchlistGroupMapper watchlistGroupMapper;

    @Resource
    private FundDataService fundDataService;

    /**
     * 获取自选列表（含实时行情和周期收益率）
     */
    public List<WatchlistItemVO> getWatchlistWithData(Long userId) {
        List<UserWatchlist> watchlist = userWatchlistMapper.findByUserId(userId);
        List<WatchlistGroup> groups = watchlistGroupMapper.findByUserId(userId);

        // 分组ID -> 分组名映射
        Map<Long, String> groupMap = new HashMap<>();
        for (WatchlistGroup group : groups) {
            groupMap.put(group.getId(), group.getGroupName());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        List<WatchlistItemVO> result = new ArrayList<>();
        for (UserWatchlist wl : watchlist) {
            try {
                WatchlistItemVO vo = new WatchlistItemVO();
                // 数据库字段
                vo.setId(wl.getId());
                vo.setFundCode(wl.getFundCode());
                vo.setFundName(wl.getFundName());
                vo.setGroupId(wl.getGroupId());
                vo.setGroupName(wl.getGroupId() != null ? groupMap.get(wl.getGroupId()) : null);
                vo.setAddNetValue(wl.getAddNetValue());
                vo.setAddTime(wl.getAddTime() != null ? sdf.format(wl.getAddTime()) : null);
                vo.setSortOrder(wl.getSortOrder());
                vo.setNotes(wl.getNotes());

                // 获取实时行情
                FundData fundData = fundDataService.getFundData(wl.getFundCode());
                if (fundData != null) {
                    vo.setUnitNetValue(fundData.getUnitNetValue());
                    vo.setEstimatedNetValue(fundData.getEstimatedNetValue());
                    vo.setEstimatedChange(fundData.getEstimatedChange());
                    vo.setLatestChange(fundData.getYesterdayChange());
                    vo.setValuationTime(fundData.getValuationTime());
                    vo.setLatestNetValueDate(fundData.getLatestNetValueDate());
                    vo.setOneWeekChange(fundData.getOneWeekChange());
                    vo.setOneMonthChange(fundData.getOneMonthChange());
                    vo.setThreeMonthChange(fundData.getThreeMonthChange());
                    vo.setSixMonthChange(fundData.getSixMonthChange());
                    vo.setOneYearChange(fundData.getOneYearChange());

                    // 计算自选以来收益率
                    vo.setReturnSinceAdded(calculateReturnSinceAdded(wl.getAddNetValue(), fundData));
                }
                result.add(vo);
            } catch (Exception e) {
                logger.warn("获取基金行情失败: fundCode={}, error={}", wl.getFundCode(), e.getMessage());
                // 即使行情失败，也返回基础数据
                WatchlistItemVO vo = new WatchlistItemVO();
                vo.setId(wl.getId());
                vo.setFundCode(wl.getFundCode());
                vo.setFundName(wl.getFundName());
                vo.setGroupId(wl.getGroupId());
                vo.setGroupName(wl.getGroupId() != null ? groupMap.get(wl.getGroupId()) : null);
                vo.setAddNetValue(wl.getAddNetValue());
                vo.setAddTime(wl.getAddTime() != null ? sdf.format(wl.getAddTime()) : null);
                vo.setSortOrder(wl.getSortOrder());
                vo.setNotes(wl.getNotes());
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 添加单个自选基金
     */
    public void addWatchlist(Long userId, String fundCode, String fundName, Long groupId, String notes) {
        if (fundCode == null || fundCode.trim().isEmpty()) {
            throw new RuntimeException("基金代码不能为空");
        }

        // 检查 200 上限
        int currentCount = userWatchlistMapper.countByUserId(userId);
        if (currentCount >= MAX_WATCHLIST_SIZE) {
            throw new RuntimeException("自选数量已达上限（" + MAX_WATCHLIST_SIZE + "只），请先移除部分自选");
        }

        // 检查重复
        UserWatchlist existing = userWatchlistMapper.findByUserIdAndFundCode(userId, fundCode);
        if (existing != null) {
            throw new RuntimeException("该基金已在自选列表中");
        }

        // 获取当前净值作为 add_net_value
        BigDecimal addNetValue = null;
        try {
            FundData fundData = fundDataService.getFundData(fundCode);
            addNetValue = resolveAddNetValue(fundData);
        } catch (Exception e) {
            logger.warn("获取基金净值失败，addNetValue 设为 null: fundCode={}, error={}", fundCode, e.getMessage());
        }

        UserWatchlist wl = new UserWatchlist();
        wl.setUserId(userId);
        wl.setFundCode(fundCode);
        wl.setFundName(fundName != null ? fundName : "");
        wl.setGroupId(groupId);
        wl.setAddNetValue(addNetValue);
        wl.setSortOrder(0);
        wl.setNotes(notes);
        userWatchlistMapper.insert(wl);

        logger.info("添加自选成功: userId={}, fundCode={}, addNetValue={}", userId, fundCode, addNetValue);
    }

    /**
     * 移除单个自选基金
     */
    public void removeWatchlist(Long userId, String fundCode) {
        int rows = userWatchlistMapper.deleteByUserIdAndFundCode(userId, fundCode);
        if (rows == 0) {
            throw new RuntimeException("未找到该自选记录");
        }
        logger.info("移除自选成功: userId={}, fundCode={}", userId, fundCode);
    }

    /**
     * 批量添加自选
     */
    public Map<String, Object> batchAddWatchlist(Long userId, List<Map<String, Object>> items) {
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<String> messages = new ArrayList<>();

        int currentCount = userWatchlistMapper.countByUserId(userId);
        int remainingSlots = MAX_WATCHLIST_SIZE - currentCount;

        if (remainingSlots <= 0) {
            throw new RuntimeException("自选数量已达上限（" + MAX_WATCHLIST_SIZE + "只），无法继续添加");
        }

        for (Map<String, Object> item : items) {
            String fundCode = (String) item.get("fundCode");
            String fundName = (String) item.get("fundName");

            if (fundCode == null || fundCode.trim().isEmpty()) {
                failedCount++;
                messages.add("基金代码为空，跳过");
                continue;
            }

            try {
                UserWatchlist existing = userWatchlistMapper.findByUserIdAndFundCode(userId, fundCode);
                if (existing != null) {
                    skippedCount++;
                    messages.add(fundCode + " 已在自选中，跳过");
                    continue;
                }

                Long groupId = item.containsKey("groupId") && item.get("groupId") != null ?
                        Long.valueOf(item.get("groupId").toString()) : null;
                String notes = (String) item.get("notes");

                addWatchlist(userId, fundCode, fundName, groupId, notes);
                successCount++;

                if (successCount >= remainingSlots) {
                    skippedCount += items.size() - successCount - skippedCount - failedCount;
                    messages.add("已达上限，剩余基金跳过");
                    break;
                }
            } catch (Exception e) {
                failedCount++;
                messages.add(fundCode + " 添加失败: " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("failedCount", failedCount);
        result.put("messages", messages);
        return result;
    }

    /**
     * 批量移除自选
     */
    public Map<String, Object> batchRemoveWatchlist(Long userId, List<String> fundCodes) {
        int deleted = userWatchlistMapper.deleteBatch(userId, fundCodes);
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deleted);
        logger.info("批量移除自选: userId={}, count={}", userId, deleted);
        return result;
    }

    /**
     * 将自选基金分配到分组（groupId 为 null 表示取消分组）
     */
    public void assignGroup(Long userId, Long watchlistId, Long groupId) {
        int rows = userWatchlistMapper.updateGroup(watchlistId, userId, groupId);
        if (rows == 0) {
            throw new RuntimeException("未找到该自选记录");
        }
        logger.info("分组分配成功: watchlistId={}, groupId={}", watchlistId, groupId);
    }

    // ==================== 分组管理 ====================

    /**
     * 获取分组列表（含各自基金数量）
     */
    public List<WatchlistGroupVO> listGroups(Long userId) {
        List<WatchlistGroup> groups = watchlistGroupMapper.findByUserId(userId);
        List<WatchlistGroupVO> result = new ArrayList<>();
        for (WatchlistGroup group : groups) {
            WatchlistGroupVO vo = new WatchlistGroupVO();
            vo.setId(group.getId());
            vo.setGroupName(group.getGroupName());
            vo.setSortOrder(group.getSortOrder());
            vo.setFundCount(userWatchlistMapper.countByUserIdAndGroupId(userId, group.getId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 创建分组
     */
    public void createGroup(Long userId, String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        WatchlistGroup group = new WatchlistGroup();
        group.setUserId(userId);
        group.setGroupName(groupName.trim());
        group.setSortOrder(0);
        watchlistGroupMapper.insert(group);
        logger.info("创建分组成功: userId={}, groupName={}", userId, groupName);
    }

    /**
     * 修改分组名称
     */
    public void updateGroup(Long userId, Long groupId, String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        int rows = watchlistGroupMapper.update(groupId, userId, groupName.trim());
        if (rows == 0) {
            throw new RuntimeException("未找到该分组");
        }
        logger.info("修改分组成功: groupId={}, groupName={}", groupId, groupName);
    }

    /**
     * 删除分组（组内基金变为未分组）
     */
    public void deleteGroup(Long userId, Long groupId) {
        // 将该分组下的所有基金设为未分组
        userWatchlistMapper.clearGroup(userId, groupId);
        // 删除分组
        int rows = watchlistGroupMapper.deleteById(groupId, userId);
        if (rows == 0) {
            throw new RuntimeException("未找到该分组");
        }
        logger.info("删除分组成功: groupId={}", groupId);
    }

    // ==================== 基金对比 ====================

    /**
     * 对比 2-5 只基金
     */
    public WatchlistCompareVO compareFunds(Long userId, List<String> fundCodes) {
        if (fundCodes == null || fundCodes.size() < 2 || fundCodes.size() > 5) {
            throw new RuntimeException("请选择 2-5 只基金进行对比");
        }

        List<WatchlistCompareVO.CompareFundItem> fundItems = new ArrayList<>();
        for (String fundCode : fundCodes) {
            try {
                UserWatchlist wl = userWatchlistMapper.findByUserIdAndFundCode(userId, fundCode);

                WatchlistCompareVO.CompareFundItem item = new WatchlistCompareVO.CompareFundItem();
                item.setFundCode(fundCode);

                FundData fundData = fundDataService.getFundData(fundCode);
                if (fundData != null) {
                    item.setFundName(fundData.getFundName() != null ? fundData.getFundName() :
                            (wl != null ? wl.getFundName() : ""));
                    item.setUnitNetValue(fundData.getUnitNetValue());
                    item.setEstimatedChange(fundData.getEstimatedChange());
                    item.setOneWeekChange(fundData.getOneWeekChange());
                    item.setOneMonthChange(fundData.getOneMonthChange());
                    item.setThreeMonthChange(fundData.getThreeMonthChange());
                    item.setSixMonthChange(fundData.getSixMonthChange());
                    item.setOneYearChange(fundData.getOneYearChange());

                    BigDecimal addNetValue = wl != null ? wl.getAddNetValue() : null;
                    item.setReturnSinceAdded(calculateReturnSinceAdded(addNetValue, fundData));
                } else if (wl != null) {
                    item.setFundName(wl.getFundName());
                }
                fundItems.add(item);
            } catch (Exception e) {
                logger.warn("对比基金时获取数据失败: fundCode={}, error={}", fundCode, e.getMessage());
                // 即使单个基金失败也继续
            }
        }

        WatchlistCompareVO result = new WatchlistCompareVO();
        result.setFunds(fundItems);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 FundData 中获取加入时的参考净值
     * 优先使用确认净值（unitNetValue），否则使用估算净值（estimatedNetValue）
     */
    private BigDecimal resolveAddNetValue(FundData fundData) {
        if (fundData == null) return null;
        String nav = fundData.getUnitNetValue();
        if (nav == null || nav.isEmpty() || "null".equals(nav)) {
            nav = fundData.getEstimatedNetValue();
        }
        if (nav != null && !nav.isEmpty() && !"null".equals(nav)) {
            try {
                return new BigDecimal(nav);
            } catch (NumberFormatException e) {
                logger.warn("净值格式异常: nav={}", nav);
                return null;
            }
        }
        return null;
    }

    /**
     * 计算自选以来收益率
     * 公式：(当前净值 - 添加时净值) / 添加时净值 × 100%
     */
    private Double calculateReturnSinceAdded(BigDecimal addNetValue, FundData fundData) {
        if (addNetValue == null || addNetValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (fundData == null) {
            return null;
        }
        try {
            String currentNavStr = fundData.getUnitNetValue();
            if (currentNavStr == null || currentNavStr.isEmpty() || "null".equals(currentNavStr)) {
                currentNavStr = fundData.getEstimatedNetValue();
            }
            if (currentNavStr == null || currentNavStr.isEmpty() || "null".equals(currentNavStr)) {
                return null;
            }
            BigDecimal currentNav = new BigDecimal(currentNavStr);
            return currentNav.subtract(addNetValue)
                    .divide(addNetValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        } catch (Exception e) {
            logger.warn("计算自选以来收益率失败: {}", e.getMessage());
            return null;
        }
    }
}
