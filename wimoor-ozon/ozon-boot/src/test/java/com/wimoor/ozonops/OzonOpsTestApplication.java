package com.wimoor.ozonops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.ops.mapper.OzonApiLogMapper;
import com.wimoor.ozon.ops.mapper.OzonOperationAuditMapper;
import com.wimoor.ozon.ops.service.impl.OzonOpsServiceImpl;

@SpringBootConfiguration
@EnableAutoConfiguration
@MapperScan(basePackageClasses = {
        OzonAuthMapper.class,
        OzonApiLogMapper.class,
        OzonOperationAuditMapper.class
})
@Import({
        OzonAuthAccessService.class,
        OzonOpsServiceImpl.class
})
public class OzonOpsTestApplication {
}
