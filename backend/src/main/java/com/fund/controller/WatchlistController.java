package com.fund.controller;

import com.fund.service.WatchlistService;
import com.fund.vo.ApiResponse;
import com.fund.vo.WatchlistCompareVO;
import com.fund.vo.WatchlistGroupVO;
import com.fund.vo.WatchlistItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistController.class);

    @Resource
    private WatchlistService watchlistService;

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            logger.warn("未从JWT找到用户ID，用户未登录！");
            return null;
        }
        return userId;
    }

    @GetMapping("/list")
    public ApiResponse<List<WatchlistItemVO>> listWatchlist(HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 获取自选列表, userId={}", userId);
        try {
            List<WatchlistItemVO> items = watchlistService.getWatchlistWithData(userId);
            return ApiResponse.success(items);
        } catch (Exception e) {
            logger.error("获取自选列表失败: {}", e.getMessage());
            return ApiResponse.error("获取自选列表失败");
        }
    }

    @PostMapping("/add")
    public ApiResponse<Map<String, Object>> addWatchlist(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        String fundCode = (String) params.get("fundCode");
        String fundName = (String) params.get("fundName");
        Long groupId = params.get("groupId") != null ? Long.valueOf(params.get("groupId").toString()) : null;
        String notes = (String) params.get("notes");
        logger.info("API: 加入自选, fundCode={}, userId={}", fundCode, userId);
        try {
            watchlistService.addWatchlist(userId, fundCode, fundName, groupId, notes);
            return ApiResponse.success("添加成功", null);
        } catch (Exception e) {
            logger.error("加入自选失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ApiResponse<Map<String, Object>> removeWatchlist(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        String fundCode = (String) params.get("fundCode");
        logger.info("API: 移除自选, fundCode={}, userId={}", fundCode, userId);
        try {
            watchlistService.removeWatchlist(userId, fundCode);
            return ApiResponse.success("移除成功", null);
        } catch (Exception e) {
            logger.error("移除自选失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/batch-add")
    public ApiResponse<Map<String, Object>> batchAddWatchlist(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> funds = (List<Map<String, Object>>) params.get("funds");
        logger.info("API: 批量添加自选, count={}, userId={}", funds != null ? funds.size() : 0, userId);
        try {
            Map<String, Object> result = watchlistService.batchAddWatchlist(userId, funds);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("批量添加自选失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/batch-remove")
    public ApiResponse<Map<String, Object>> batchRemoveWatchlist(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        @SuppressWarnings("unchecked")
        List<String> fundCodes = (List<String>) params.get("fundCodes");
        logger.info("API: 批量移除自选, count={}, userId={}", fundCodes != null ? fundCodes.size() : 0, userId);
        try {
            Map<String, Object> result = watchlistService.batchRemoveWatchlist(userId, fundCodes);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("批量移除自选失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/group")
    public ApiResponse<Map<String, Object>> assignGroup(@PathVariable("id") Long id,
                                                         @RequestBody Map<String, Object> params,
                                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        Long groupId = params.get("groupId") != null ? Long.valueOf(params.get("groupId").toString()) : null;
        logger.info("API: 分配分组, watchlistId={}, groupId={}, userId={}", id, groupId, userId);
        try {
            watchlistService.assignGroup(userId, id, groupId);
            return ApiResponse.success("分组设置成功", null);
        } catch (Exception e) {
            logger.error("分配分组失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 分组管理 ====================

    @GetMapping("/groups")
    public ApiResponse<List<WatchlistGroupVO>> listGroups(HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 获取分组列表, userId={}", userId);
        try {
            List<WatchlistGroupVO> groups = watchlistService.listGroups(userId);
            return ApiResponse.success(groups);
        } catch (Exception e) {
            logger.error("获取分组列表失败: {}", e.getMessage());
            return ApiResponse.error("获取分组列表失败");
        }
    }

    @PostMapping("/groups")
    public ApiResponse<Map<String, Object>> createGroup(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        String groupName = (String) params.get("groupName");
        logger.info("API: 创建分组, groupName={}, userId={}", groupName, userId);
        try {
            watchlistService.createGroup(userId, groupName);
            return ApiResponse.success("创建成功", null);
        } catch (Exception e) {
            logger.error("创建分组失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/groups/{id}")
    public ApiResponse<Map<String, Object>> updateGroup(@PathVariable("id") Long id,
                                                         @RequestBody Map<String, Object> params,
                                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        String groupName = (String) params.get("groupName");
        logger.info("API: 修改分组, groupId={}, groupName={}, userId={}", id, groupName, userId);
        try {
            watchlistService.updateGroup(userId, id, groupName);
            return ApiResponse.success("修改成功", null);
        } catch (Exception e) {
            logger.error("修改分组失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/groups/{id}")
    public ApiResponse<Map<String, Object>> deleteGroup(@PathVariable("id") Long id,
                                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 删除分组, groupId={}, userId={}", id, userId);
        try {
            watchlistService.deleteGroup(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            logger.error("删除分组失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 基金对比 ====================

    @PostMapping("/compare")
    public ApiResponse<WatchlistCompareVO> compareFunds(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getUserId(request);
        @SuppressWarnings("unchecked")
        List<String> fundCodes = (List<String>) params.get("fundCodes");
        logger.info("API: 基金对比, fundCodes={}, userId={}", fundCodes, userId);
        try {
            WatchlistCompareVO result = watchlistService.compareFunds(userId, fundCodes);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("基金对比失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}
