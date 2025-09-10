package org.dromara.hm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单位枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum UnitEnum {

    /**
     * 无单位
     */
    NONE("/", "无", "/", 0),

    /**
     * 分贝
     */
    DECIBEL("dB", "分贝", "/", 1),

    /**
     * 分贝毫瓦
     */
    DBM("dBm", "分贝毫瓦", "功率", 2),

    /**
     * 分贝毫伏
     */
    DBMV("dBmV", "分贝毫伏", "电压", 3),

    /**
     * 分贝微伏
     */
    DBUV("dBμV", "分贝微伏", "电压", 4),

    /**
     * 伏特
     */
    VOLT("V", "伏", "电压", 5),

    /**
     * 毫伏
     */
    MILLIVOLT("mV", "毫伏", "电压", 6),

    /**
     * 微伏
     */
    MICROVOLT("μV", "微伏", "电压", 7),

    /**
     * 百分比
     */
    PERCENT("%", "百分比", "比值", 8),

    /**
     * 安培
     */
    AMPERE("A", "安[培]", "电流", 9),

    /**
     * 毫安
     */
    MILLIAMPERE("mA", "毫安", "电流", 10),

    /**
     * 微安
     */
    MICROAMPERE("μA", "微安", "电流", 11),

    /**
     * 欧姆
     */
    OHM("Ω", "欧[姆]", "电阻", 12),

    /**
     * 毫欧
     */
    MILLIOHM("mΩ", "毫欧", "电阻", 13),

    /**
     * 微欧
     */
    MICROOHM("μΩ", "微欧", "电阻", 14),

    /**
     * 米每二次方秒（加速度）
     */
    METER_PER_SECOND_SQUARED("m/s²", "米每二次方秒", "加速度", 15),

    /**
     * 毫米
     */
    MILLIMETER("mm", "毫米", "长度", 16),

    /**
     * 摄氏度
     */
    CELSIUS("℃", "摄氏度", "温度", 17),

    /**
     * 华氏度
     */
    FAHRENHEIT("℉", "华氏度", "温度", 18),

    /**
     * 帕斯卡
     */
    PASCAL("Pa", "帕", "压力", 19),

    /**
     * 库仑
     */
    COULOMB("C", "库[伦]", "电荷量", 20),

    /**
     * 毫库
     */
    MILLICOULOMB("mC", "毫库", "电荷量", 21),

    /**
     * 微库
     */
    MICROCOULOMB("μC", "微库", "电荷量", 22),

    /**
     * 纳库
     */
    NANOCOULOMB("nC", "纳库", "电荷量", 23),

    /**
     * 皮库
     */
    PICOCOULOMB("pC", "皮库", "电荷量", 24),

    /**
     * 米每秒
     */
    METER_PER_SECOND("m/s", "米每秒", "速度", 25),

    /**
     * 千欧
     */
    KILOOHM("kΩ", "千欧", "电阻", 26),

    /**
     * 兆欧
     */
    MEGAOHM("MΩ", "兆欧", "电阻", 27),

    /**
     * 吉欧
     */
    GIGAOHM("GΩ", "吉欧", "电阻", 28),

    /**
     * 太欧
     */
    TERAOHM("TΩ", "太欧", "电阻", 29),

    /**
     * 赫兹
     */
    HERTZ("Hz", "赫兹", "频率", 30),

    /**
     * 亨利
     */
    HENRY("H", "亨", "电感量", 31),

    /**
     * 毫亨
     */
    MILLIHENRY("mH", "毫亨", "电感量", 32),

    /**
     * 法拉
     */
    FARAD("F", "法[拉]", "电容量", 33),

    /**
     * 毫法
     */
    MILLIFARAD("mF", "毫法", "电容量", 34),

    /**
     * 微法
     */
    MICROFARAD("μF", "微法", "电容量", 35),

    /**
     * 纳法
     */
    NANOFARAD("nF", "纳法", "电容量", 36),

    /**
     * 皮法
     */
    PICOFARAD("pF", "皮法", "电容量", 37),

    /**
     * 秒
     */
    SECOND("s", "秒", "时间", 38),

    /**
     * 毫秒
     */
    MILLISECOND("ms", "毫秒", "时间", 39),

    /**
     * 微秒
     */
    MICROSECOND("μs", "微秒", "时间", 40),

    /**
     * 千帕
     */
    KILOPASCAL("kPa", "千帕", "压力", 41),

    /**
     * 兆帕
     */
    MEGAPASCAL("MPa", "兆帕", "压力", 42),

    /**
     * 微升每升
     */
    MICROLITER_PER_LITER("μL/L", "微升每升", "比值", 43),

    /**
     * 度
     */
    DEGREE("°", "度", "角度", 44);

    /**
     * 单位符号
     */
    private final String symbol;

    /**
     * 单位名称
     */
    private final String name;

    /**
     * 对应状态量
     */
    private final String physicalQuantity;

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 根据编码获取枚举
     *
     * @param code 单位编码
     * @return 单位枚举，如果未找到则返回null
     */
    public static UnitEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UnitEnum unit : values()) {
            if (unit.getCode().equals(code)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 根据符号获取枚举
     *
     * @param symbol 单位符号
     * @return 单位枚举，如果未找到则返回null
     */
    public static UnitEnum getBySymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        for (UnitEnum unit : values()) {
            if (unit.getSymbol().equals(symbol)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 单位名称
     * @return 单位枚举，如果未找到则返回null
     */
    public static UnitEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (UnitEnum unit : values()) {
            if (unit.getName().equals(name)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 根据物理量获取单位列表
     *
     * @param physicalQuantity 物理量类型
     * @return 该物理量对应的单位列表
     */
    public static java.util.List<UnitEnum> getByPhysicalQuantity(String physicalQuantity) {
        if (physicalQuantity == null) {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(values())
            .filter(unit -> unit.getPhysicalQuantity().equals(physicalQuantity))
            .collect(Collectors.toList());
    }

    /**
     * 获取单位映射（用于前端下拉选择等）
     *
     * @return Map<code, name>
     */
    public static Map<Integer, String> getUnitMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                UnitEnum::getCode,
                UnitEnum::getName,
                (existing, replacement) -> existing
            ));
    }

    /**
     * 获取符号映射
     *
     * @return Map<code, symbol>
     */
    public static Map<Integer, String> getSymbolMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                UnitEnum::getCode,
                UnitEnum::getSymbol,
                (existing, replacement) -> existing
            ));
    }

    /**
     * 获取物理量映射
     *
     * @return Map<code, physicalQuantity>
     */
    public static Map<Integer, String> getPhysicalQuantityMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                UnitEnum::getCode,
                UnitEnum::getPhysicalQuantity,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("Unit{symbol='%s', name='%s', physicalQuantity='%s', code=%d}",
            symbol, name, physicalQuantity, code);
    }
}
