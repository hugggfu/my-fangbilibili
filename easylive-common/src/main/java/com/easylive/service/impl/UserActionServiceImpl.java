package com.easylive.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.EsSearchComponent;
import com.easylive.entity.config.RabbitMQConfig;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserInfoMapper;
import com.easylive.mappers.VideoCommentMapper;
import com.easylive.mappers.VideoInfoMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.UserActionMapper;
import com.easylive.service.UserActionService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 用户行为 点赞、评论 业务接口实现
 */

@Service("userActionService")
@Slf4j
public class UserActionServiceImpl implements UserActionService {

	@Resource
	private UserActionMapper<UserAction, UserActionQuery> userActionMapper;

    @Resource
    private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

    @Resource
    private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    RabbitTemplate rabbitTemplate;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserAction> findListByParam(UserActionQuery param) {
		return this.userActionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserActionQuery param) {
		return this.userActionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserAction> findListByPage(UserActionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserAction> list = this.findListByParam(param);
		PaginationResultVO<UserAction> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserAction bean) {
		return this.userActionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserAction bean, UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.deleteByParam(param);
	}

	/**
	 * 根据ActionId获取对象
	 */
	@Override
	public UserAction getUserActionByActionId(Integer actionId) {
		return this.userActionMapper.selectByActionId(actionId);
	}

	/**
	 * 根据ActionId修改
	 */
	@Override
	public Integer updateUserActionByActionId(UserAction bean, Integer actionId) {
		return this.userActionMapper.updateByActionId(bean, actionId);
	}

	/**
	 * 根据ActionId删除
	 */
	@Override
	public Integer deleteUserActionByActionId(Integer actionId) {
		return this.userActionMapper.deleteByActionId(actionId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId获取对象
	 */
	@Override
	public UserAction getUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId修改
	 */
	@Override
	public Integer updateUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(UserAction bean, String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.updateByVideoIdAndCommentIdAndActionTypeAndUserId(bean, videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId删除
	 */
	@Override
	public Integer deleteUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.deleteByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAction(UserAction bean) {
        VideoInfo videoInfo = videoInfoMapper.selectByVideoId(bean.getVideoId());
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        bean.setVideoUserId(videoInfo.getUserId());

        UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(bean.getActionType());
        if (actionTypeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        bean.setActionTime(new Date());

        // ================== 改造开始 ==================
        //如果是 点赞 或 收藏，直接发 MQ，然后返回
        if (UserActionTypeEnum.VIDEO_LIKE == actionTypeEnum || UserActionTypeEnum.VIDEO_COLLECT == actionTypeEnum) {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ACTION_EXCHANGE,
                    RabbitMQConfig.ACTION_ROUTING_KEY,
                    bean
            );
            return; // 🚀 直接返回，后续逻辑交给消费者
        }
        // ================== 改造结束 ==================
        UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(), bean.getActionType(),
                     bean.getUserId());
        switch (actionTypeEnum) {

            case VIDEO_COIN:
                if (videoInfo.getUserId().equals(bean.getUserId())) {
                    throw new BusinessException("UP主不能给自己投币");
                }
                if (dbAction != null) {
                    throw new BusinessException("对本稿件的投币枚数已用完");
                }
                //减少自己的硬币
                Integer updateCount = userInfoMapper.updateCoinCountInfo(bean.getUserId(), -bean.getActionCount());
                if (updateCount == 0) {
                    throw new BusinessException("币不够");
                }
                //增加up主的硬币
                updateCount = userInfoMapper.updateCoinCountInfo(videoInfo.getUserId(), bean.getActionCount());
                if (updateCount == 0) {
                    throw new BusinessException("投币失败");
                }
                userActionMapper.insert(bean);
                videoInfoMapper.updateCountInfo(bean.getVideoId(), actionTypeEnum.getField(), bean.getActionCount());
                break;
            //评论
            case COMMENT_LIKE:

            case COMMENT_HATE:
                UserActionTypeEnum opposeTypeEnum = UserActionTypeEnum.COMMENT_LIKE == actionTypeEnum ? UserActionTypeEnum.COMMENT_HATE : UserActionTypeEnum.COMMENT_LIKE;
                UserAction opposeAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(),
                        opposeTypeEnum.getType(), bean.getUserId());
                if (opposeAction != null) {
                    userActionMapper.deleteByActionId(opposeAction.getActionId());
                }

                if (dbAction != null) {
                    userActionMapper.deleteByActionId(dbAction.getActionId());
                } else {
                    userActionMapper.insert(bean);
                }
                Integer changeCount = dbAction == null ? 1 : -1;
                Integer opposeChangeCount = changeCount * -1;
                videoCommentMapper.updateCountInfo(bean.getCommentId(),
                        actionTypeEnum.getField(),
                        changeCount,
                        opposeAction == null ? null : opposeTypeEnum.getField(),
                        opposeChangeCount);
                break;
        }
    }

    /**
     * 监听 MQ 队列，处理点赞/收藏的数据库操作
     */
    @RabbitListener(queues = RabbitMQConfig.ACTION_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void consumeAction(UserAction bean, Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("消费行为消息: type={}, videoId={}, userId={}", bean.getActionType(), bean.getVideoId(), bean.getUserId());

            UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(bean.getActionType());

            // 1. 在这里查数据库，判断是新增还是取消 (避免并发问题)
            UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(
                    bean.getVideoId(), bean.getCommentId(), bean.getActionType(), bean.getUserId());
            // 2. 执行新增或删除 (Toggle逻辑)
            if (dbAction != null) {
                userActionMapper.deleteByActionId(dbAction.getActionId());
            } else {
                userActionMapper.insert(bean);
            }
            // 3. 更新视频主表计数
            Integer changeCount = dbAction == null ? 1 : -1;
            videoInfoMapper.updateCountInfo(bean.getVideoId(), actionTypeEnum.getField(), changeCount);
            // 4. 更新 ES (如果是收藏)
            if (actionTypeEnum == UserActionTypeEnum.VIDEO_COLLECT) {
                esSearchComponent.updateDocCount(bean.getVideoId(), SearchOrderTypeEnum.VIDEO_COLLECT.getField(), changeCount);
            }

            // 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理点赞收藏消息失败", e);
            try {
                // 发生异常，重回队列 (根据业务可以是 false 丢弃)
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}