package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2018/3/24.
 * 向上注入Controller中接口为IUserService,属性的名字为iUserService
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {


    @Autowired
    private UserMapper userMapper;

    //portal

    /**
     *用户登陆
     * @param username  :用户名
     * @param password  :密码
     * @return  :返回服务端响应类
     */
    @Override
    public ServerResponse<User> login(String username, String password) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        //密码md5加密
        String md5Password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username, md5Password);

        //用户名存在,密码错误
        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        //密码置为空,避免信息泄露
        user.setPassword(StringUtils.EMPTY);
        //返回登录成功信息和user对象
        return ServerResponse.createBySuccess("登陆成功", user);
    }

    /**
     * 用户注册
     * @param user  :用户对象
     * @return
     */
    @Override
    public ServerResponse<String> register(User user) {
        ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        //校验用户名是否存在
        if (!validResponse.isSuccess()) {
            return validResponse;
        }
        //校验email是否存在
        validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }

        //设置为普通用户
        user.setRole(Const.Role.ROLE_CUSTOMER);

        //对密码进行MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        //向数据库插入用户
        int resultCount = userMapper.insert(user);

        //插入失败
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }

        //成功
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 对用户名或者邮箱是否有效的校验
     * 此处一个bug: type不为空,无论是什么都会"校验成功"
     * @param str   :value
     * @param type  :email或username
     * @return
     */
    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        //StringUtils.isNotBlank(" ") = false
        if (StringUtils.isNotBlank(str) && StringUtils.isNotBlank(type)) {
            //开始校验
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }

        return ServerResponse.createBySuccessMessage("校验成功");
    }

    /**
     *忘记密码: 根据用户名返回密码提示问题
     * @param username  :用户名
     * @return
     */
    @Override
    public ServerResponse selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            //question放入响应体中的data
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回是密码的问题是空的");
    }

    /**
     * 校验问题答案
     * @param username  :用户名
     * @param question  :问题
     * @param answer    :答案
     * @return
     */
    @Override
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username, question, answer);

        //说明问题及问题答案是这个用户的，并且是正确的
        if (resultCount > 0) {
            //利用uuid生成令牌
            String forgetToken = UUID.randomUUID().toString();
            //将token放入本地cache,并设置其有效期
            //将前缀"token_"抽象理解成namespace
            TokenCache.setKey(TokenCache.TOKEN_FREFIX + username, forgetToken);
            //将token放入响应体data
            return ServerResponse.createBySuccess(forgetToken);
        }

        return ServerResponse.createByErrorMessage("问题答案错误");
    }

    /**
     * 忘记密码: 重置密码
     * @param username      :用户名
     * @param passwordNew   :新密码
     * @param forgetToken   :令牌
     * @return
     */
    @Override
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        //从cache里面获取token
        String token = TokenCache.getKey(TokenCache.TOKEN_FREFIX + username);
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }
        //StringUtils.equals避免字符串为null时，报空指针异常
        if (StringUtils.equals(forgetToken, token)) {
            //更新密码
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUnsername(username, md5Password);
            if (rowCount > 0) {
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        } else {
            return ServerResponse.createByErrorMessage("token错误，请重新获取重置密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    /**
     *重置密码
     * @param passwordOld   :旧密码
     * @param passwordNew   :新密码
     * @param user          :当前用户对象
     * @return
     */
    @Override
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        //  防止横向越权，要检验一下这个用户的旧密码，一定要指定是这个用户,因为我们会查询到一个count(1)
        //  如果不指定Id,那么结果就是true count > 0，那么其他人不断试重置密码接口便可更改密码
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        //根据主键选择性更新,即哪个属性值不为空更新哪个
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0) {
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    /**
     * 更新个人用户信息
     * @param user  :用户对象
     * @return
     */
    @Override
    public ServerResponse<User> updateInormation(User user) {
        //username不能被更新
        //email也要校验，校验新的email是否已经存在，并且存在的email如果相同的话，不能是我们当前的这个用户的。
        if(StringUtils.isBlank(user.getEmail()))
            return ServerResponse.createByErrorMessage("email不能为空");

        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("email已存在,请更换email再尝试更新");
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        //根据主键选择性更新,即哪个属性值不为空更新哪个
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("更新个人信息成功", updateUser);
        }

        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    /**
     * 获取用户详细信息
     * @param userId    :用户id即主键
     * @return
     */
    @Override
    public ServerResponse<User> getInformation(Integer userId) {
        //通过主键查找
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        //将密码置空，防止信息泄露
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    //backend

    /**
     * 校验是否是管理员
     * @param user
     * @return
     */
    @Override
    public ServerResponse checkAdminRole(User user) {
        if (user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    @Override
    public ServerResponse<PageInfo> manageList(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<User> userList = userMapper.selectAllUser();
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            user.setEmail(user.getEmail().replaceAll("(\\w?)(\\w+)(\\w)(@\\w+\\.[a-z]+(\\.[a-z]+)?)", "$1****$3$4"));
            user.setPhone(user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            userList.set(i,user);
        }
        PageInfo pageResult = new PageInfo(userList);
        return ServerResponse.createBySuccess(pageResult);
    }
}

