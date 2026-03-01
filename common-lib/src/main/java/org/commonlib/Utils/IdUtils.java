package org.commonlib.Utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

//@Component
public class IdUtils {
    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    public synchronized long nextId() {
        return snowflake.nextId();
    }
}
