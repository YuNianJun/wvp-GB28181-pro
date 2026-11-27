package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.service.bean.PlayPreset;
import com.genersoft.iot.vmp.service.bean.PlayPresetItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 分屏监控预案 Mapper
 * 说明：查询时显式指定别名， 避免依赖 MyBatis 的下划线转驼峰配置
 *
 * @author wvp
 * @date 2026-11-25
 */
@Mapper
public interface PlayPresetMapper {

    String SELECT_COLUMNS = "SELECT p.id, p.user_id AS userId, p.name, p.split_num AS splitNum, "
            + "p.is_default AS isDefault, p.create_time AS createTime, p.update_time AS updateTime ";

    /**
     * 新增预案
     *
     * @param preset 预案信息
     * @return 影响行数
     */
    @Insert("INSERT INTO wvp_play_preset (user_id, name, split_num, is_default, create_time, update_time) "
            + "VALUES (#{userId}, #{name}, #{splitNum}, #{isDefault}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(PlayPreset preset);

    /**
     * 更新预案， 只能更新自己的预案
     *
     * @param preset 预案信息
     * @return 影响行数
     */
    @Update("UPDATE wvp_play_preset SET update_time = #{updateTime}, name = #{name}, split_num = #{splitNum} "
            + "WHERE id = #{id} AND user_id = #{userId}")
    int update(PlayPreset preset);

    /**
     * 删除预案， 只能删除自己的预案
     *
     * @param presetId 预案ID
     * @param userId   用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM wvp_play_preset WHERE id = #{presetId} AND user_id = #{userId}")
    int delete(@Param("presetId") int presetId, @Param("userId") int userId);

    /**
     * 查询单个预案
     *
     * @param presetId 预案ID
     * @param userId   用户ID
     * @return 预案信息
     */
    @Select(SELECT_COLUMNS + "FROM wvp_play_preset p WHERE p.id = #{presetId} AND p.user_id = #{userId}")
    PlayPreset get(@Param("presetId") int presetId, @Param("userId") int userId);

    /**
     * 查询用户的全部预案， 附带通道数量， 用于下拉列表展示
     *
     * @param userId 用户ID
     * @return 预案列表
     */
    @Select(SELECT_COLUMNS + ", (SELECT COUNT(1) FROM wvp_play_preset_item i WHERE i.preset_id = p.id) AS channelCount "
            + "FROM wvp_play_preset p WHERE p.user_id = #{userId} ORDER BY p.update_time DESC, p.id DESC")
    List<PlayPreset> queryByUser(@Param("userId") int userId);

    /**
     * 查询用户的默认预案
     *
     * @param userId 用户ID
     * @return 默认预案， 不存在时返回 null
     */
    @Select(SELECT_COLUMNS + "FROM wvp_play_preset p WHERE p.user_id = #{userId} AND p.is_default = 1 LIMIT 1")
    PlayPreset getDefault(@Param("userId") int userId);

    /**
     * 查询预案明细， 联表带出通道名称
     *
     * @param presetId 预案ID
     * @return 明细列表
     */
    @Select("SELECT i.id, i.preset_id AS presetId, i.channel_id AS channelId, i.`sort` AS `sort`, "
            + "coalesce(c.gb_name, c.name) AS channelName "
            + "FROM wvp_play_preset_item i LEFT JOIN wvp_device_channel c ON c.id = i.channel_id "
            + "WHERE i.preset_id = #{presetId} ORDER BY i.`sort`")
    List<PlayPresetItem> queryItems(@Param("presetId") int presetId);

    /**
     * 批量新增预案明细
     *
     * @param itemList 明细列表
     * @return 影响行数
     */
    @Insert("<script>"
            + "INSERT INTO wvp_play_preset_item (preset_id, channel_id, `sort`) VALUES "
            + "<foreach collection='itemList' item='item' separator=','>"
            + "(#{item.presetId}, #{item.channelId}, #{item.sort})"
            + "</foreach>"
            + "</script>")
    int batchAddItems(@Param("itemList") List<PlayPresetItem> itemList);

    /**
     * 删除预案下的全部明细
     *
     * @param presetId 预案ID
     * @return 影响行数
     */
    @Delete("DELETE FROM wvp_play_preset_item WHERE preset_id = #{presetId}")
    int cleanItems(@Param("presetId") int presetId);

    /**
     * 清除用户的默认预案标记
     *
     * @param userId     用户ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE wvp_play_preset SET is_default = 0, update_time = #{updateTime} "
            + "WHERE user_id = #{userId} AND is_default = 1")
    int cleanDefault(@Param("userId") int userId, @Param("updateTime") String updateTime);

    /**
     * 将指定预案设为默认
     *
     * @param presetId   预案ID
     * @param userId     用户ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE wvp_play_preset SET is_default = 1, update_time = #{updateTime} "
            + "WHERE id = #{presetId} AND user_id = #{userId}")
    int setDefault(@Param("presetId") int presetId, @Param("userId") int userId,
                   @Param("updateTime") String updateTime);

    /**
     * 统计同一用户下重名的预案数量
     *
     * @param userId    用户ID
     * @param name      预案名称
     * @param excludeId 排除的预案ID， 新增时传 -1
     * @return 重名数量
     */
    @Select("SELECT COUNT(1) FROM wvp_play_preset WHERE user_id = #{userId} AND name = #{name} AND id != #{excludeId}")
    int countByName(@Param("userId") int userId, @Param("name") String name, @Param("excludeId") int excludeId);
}
