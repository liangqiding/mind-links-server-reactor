package entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * date: 2021-01-22 10:58
 * description
 *
 * @author qiDing
 */
@Data
@Accessors(chain = true)
public class KafkaTem {

    private Long timestamp;

    private Object payload;

}
