package exception;

import annotation.Desc;
import exception.enums.LinksExceptionEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: QiDing
 * @date : 2020/11/26 0026 10:09
 * description: TODO
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Desc("自定义异常")
public class LinksException extends RuntimeException {

    @Desc("错误码")
    private Integer code;

    @Desc("错误反馈")
    private String msg;

    public LinksException(Integer code, String msg){
        super(msg);
        this.code = code;
        this.msg = msg;
    }
    public LinksException(Integer code){
        super(LinksExceptionEnum.getMsgByCode(code));
        this.code = code;
        this.msg = LinksExceptionEnum.getMsgByCode(code);
    }
    public LinksException(String msg){
        super(msg);
        this.msg = msg;
    }
}