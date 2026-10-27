package com.ruoyi.common.utils.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel 数据格式化处理器接口
 *
 * <p>在 @Excel 注解中通过 handler 参数指定自定义处理器，
 * 实现本接口可在 Excel 导入/导出时对每个单元格进行二次转换（如枚举值转文字、脱敏、加密字段解密等）。
 *
 * <p>实现类需提供空构造函数供反射实例化，处理器参数通过 args 数组传入。
 *
 * @author ruoyi
 */
public interface ExcelHandlerAdapter
{
    /**
     * 格式化
     * 
     * @param value 单元格数据值
     * @param args excel注解args参数组
     * @param cell 单元格对象
     * @param wb 工作簿对象
     *
     * @return 处理后的值
     */
    Object format(Object value, String[] args, Cell cell, Workbook wb);
}
