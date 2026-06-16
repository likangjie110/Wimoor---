package com.wimoor.ozon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import com.wimoor.OzonApplication;

class OzonApplicationMapperScanTests {

    @Test
    void applicationScansOzonAndCommonMappers() {
        MapperScan mapperScan = OzonApplication.class.getAnnotation(MapperScan.class);

        assertNotNull(mapperScan);
        assertArrayEquals(new String[] {
                "com.wimoor.ozon.**.mapper",
                "com.wimoor.common.mapper"
        }, mapperScan.value());
    }
}
