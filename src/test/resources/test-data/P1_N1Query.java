package com.example.testdata;

import java.util.ArrayList;
import java.util.List;

/**
 * P1优先级测试用例：N+1查询问题
 *
 * 预期问题：
 * - Priority: P1
 * - Severity: MAJOR
 * - Category: 性能问题
 * - Description: N+1查询问题，导致数据库查询次数过多
 * - Impact: 当订单数量超过100时，响应时间超过5秒
 */
public class P1_N1Query {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;

    /**
     * N+1查询问题 - 在循环中查询关联数据
     *
     * @param userId 用户ID
     * @return 用户的所有订单（包含订单项）
     */
    public List<Order> getUserOrdersWithItems(Long userId) {
        // 第1次查询：获取用户的所有订单
        List<Order> orders = orderRepository.findByUserId(userId);

        // ❌ N+1问题：对每个订单再查询一次订单项
        // 如果有100个订单，这里会执行100次额外查询
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }

        return orders;
    }

    /**
     * 另一个N+1问题 - 嵌套循环查询
     */
    public List<Order> getOrdersWithDetails(List<Long> orderIds) {
        List<Order> orders = new ArrayList<>();

        // ❌ 对每个ID执行一次查询
        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId);

            // ❌ 再对每个订单的项目执行查询
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            order.setItems(items);

            // ❌ 还要查询每个订单的用户信息
            User user = orderRepository.findUserByOrderId(orderId);
            order.setUser(user);

            orders.add(order);
        }

        return orders;
    }

    /**
     * 第三个性能问题 - 未使用批量操作
     */
    public void updateOrderStatuses(List<Long> orderIds, String newStatus) {
        // ❌ 逐个更新，而不是使用批量更新
        for (Long orderId : orderIds) {
            orderRepository.updateStatus(orderId, newStatus);
        }
    }

    // 模拟的Repository接口
    interface OrderRepository {
        List<Order> findByUserId(Long userId);
        Order findById(Long orderId);
        User findUserByOrderId(Long orderId);
        void updateStatus(Long orderId, String status);
    }

    interface OrderItemRepository {
        List<OrderItem> findByOrderId(Long orderId);
    }

    static class Order {
        private Long id;
        private List<OrderItem> items;
        private User user;
        public Long getId() { return id; }
        public void setItems(List<OrderItem> items) { this.items = items; }
        public void setUser(User user) { this.user = user; }
    }

    static class OrderItem {
        private Long id;
        private String productName;
    }

    static class User {
        private Long id;
        private String username;
    }
}
