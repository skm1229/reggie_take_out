package com.skm.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.User;
import com.skm.reggie.service.UserService;
import com.skm.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        //获取手机号
        String phone = user.getPhone();
        if(StringUtils.isNotEmpty(phone)) {

            //随机生成4位数验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("手机号= {},验证码 = {}",phone,code);
            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("验证码短信","\n" + "SMS_273825569",phone,code);
            //session.setAttribute(phone,code);
            //从生成的验证码放到Redis中，并设置有效期为3分钟
            redisTemplate.opsForValue().set(phone,code,3, TimeUnit.MINUTES);

            return R.success("手机验证码发送成功");
        }
        return R.error("短信发送失败");

    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> sendMsg(@RequestBody Map map, HttpSession session) {
        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从Session中获取保存的验证码
        //Object codeInSession = session.getAttribute(phone);
        //从Redis中获取缓存的验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);
        //进行验证码的比对(页面提交的验证码和Session保存的验证码比对)
        if(codeInSession != null && codeInSession.equals(code)) {
            //如果能够比对成,说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(queryWrapper);
            if(user == null) {
                //判断当前手机号对应的用户是否为新用户,如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }

        //如果用户登录成功,从Redis中删除缓存的验证码
        redisTemplate.delete(phone);
        return R.error("登录失败~~");

    }

    /**
     * 移到端用户退出登录
     * @return
     */
    @PostMapping("/loginout")
    public R<String> loginout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return R.success("退出登录~~");
    }
}
