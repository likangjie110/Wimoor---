package com.wimoor.erp.order.service.impl;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.erp.order.mapper.OrderMapper;
import com.wimoor.erp.order.mapper.OrderPlatformMapper;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertCommand;
import com.wimoor.erp.order.pojo.dto.OzonErpOrderUpsertResult;
import com.wimoor.erp.order.pojo.entity.Order;
import com.wimoor.erp.order.pojo.entity.OrderPlatform;
import com.wimoor.erp.util.CountryUtil;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OzonOrderBridgeService {

    private static final String PLATFORM_NAME = "Ozon";

    private final OrderPlatformMapper orderPlatformMapper;
    private final OrderMapper orderMapper;

    public OzonErpOrderUpsertResult upsert(OzonErpOrderUpsertCommand command) {
        validate(command);
        Date now = new Date();
        OrderPlatform platform = ensurePlatform(command.getShopId(), now);
        Order order = orderMapper.selectOne(new QueryWrapper<Order>()
                .eq("shopid", command.getShopId())
                .eq("platform_id", platform.getId())
                .eq("order_id", command.getPostingNumber())
                .eq("sku", command.getMaterialSku()));
        boolean isNew = order == null;
        if (isNew) {
            order = new Order();
            order.setId(nextId());
        }
        fillOrder(order, platform, command, now);
        if (isNew) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }
        return new OzonErpOrderUpsertResult(order.getId());
    }

    private OrderPlatform ensurePlatform(String shopId, Date now) {
        OrderPlatform platform = orderPlatformMapper.selectOne(new QueryWrapper<OrderPlatform>()
                .eq("shopid", shopId)
                .eq("name", PLATFORM_NAME));
        if (platform != null) {
            return platform;
        }
        platform = new OrderPlatform();
        platform.setId(nextId());
        platform.setName(PLATFORM_NAME);
        platform.setShopid(shopId);
        platform.setDisabled(false);
        platform.setOpttime(now);
        orderPlatformMapper.insert(platform);
        return platform;
    }

    private void fillOrder(Order order, OrderPlatform platform, OzonErpOrderUpsertCommand command, Date now) {
        order.setPlatformId(platform.getId());
        order.setOrderId(command.getPostingNumber().trim());
        order.setSku(command.getMaterialSku().trim());
        order.setWarehouseid(trim(command.getWarehouseId()));
        order.setThirdpartyWarehouseid(trim(command.getThirdpartyWarehouseId()));
        order.setCountry(normalizeCountry(command.getCountry()));
        order.setCurrency(trim(command.getCurrency()));
        order.setQuantity(command.getQuantity());
        order.setPrice(command.getPrice());
        order.setPurchaseDate(command.getPurchaseDate() == null ? now : command.getPurchaseDate());
        order.setShopid(command.getShopId().trim());
        order.setIsout(false);
        order.setOpttime(now);
    }

    private String normalizeCountry(String country) {
        if (StrUtil.isBlank(country)) {
            return "RU";
        }
        String normalized = CountryUtil.getCountryCode(country);
        if (StrUtil.isBlank(normalized)) {
            normalized = CountryUtil.getCountryCodeEn(country);
        }
        return StrUtil.isBlank(normalized) ? country.trim() : normalized;
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private void validate(OzonErpOrderUpsertCommand command) {
        if (command == null || StrUtil.isBlank(command.getShopId())) {
            throw new IllegalArgumentException("shopId不能为空");
        }
        if (StrUtil.isBlank(command.getPostingNumber())) {
            throw new IllegalArgumentException("postingNumber不能为空");
        }
        if (StrUtil.isBlank(command.getMaterialSku())) {
            throw new IllegalArgumentException("materialSku不能为空");
        }
        if (command.getQuantity() == null || command.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity必须大于0");
        }
    }

    private String nextId() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
