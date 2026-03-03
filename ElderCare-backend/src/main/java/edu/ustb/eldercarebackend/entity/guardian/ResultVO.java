package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultVO {
    private int code;
    private String msg;
    private Object data;

    public static ResultVO success(Object data) {
        ResultVO result = new ResultVO();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static ResultVO success(String msg) {
        ResultVO result = new ResultVO();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    public static ResultVO success(String msg, Object data) {
        ResultVO result = new ResultVO();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public static ResultVO fail(String msg) {
        ResultVO result = new ResultVO();
        result.setCode(400);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

}
