/*
 * Copyleft (c) 2021 ksqeib,CaaMoe. All rights reserved.
 * @author  ksqeib <ksqeib@dalao.ink> <https://github.com/ksqeib445>
 * @author  CaaMoe <miaolio@qq.com> <https://github.com/CaaMoe>
 * @github  https://github.com/CaaMoe/MultiLogin
 *
 * moe.caa.multilogin.core.auth.Verifier
 *
 * Use of this source code is governed by the GPLv3 license that can be found via the following link.
 * https://github.com/CaaMoe/MultiLogin/blob/master/LICENSE
 */

package moe.caa.multilogin.core.auth;

import moe.caa.multilogin.core.data.User;
import moe.caa.multilogin.core.data.database.handler.CacheWhitelistDataHandler;
import moe.caa.multilogin.core.data.database.handler.UserDataHandler;
import moe.caa.multilogin.core.impl.ISender;
import moe.caa.multilogin.core.language.LanguageKeys;
import moe.caa.multilogin.core.logger.LoggerLevel;
import moe.caa.multilogin.core.logger.MultiLogger;
import moe.caa.multilogin.core.main.MultiCore;
import moe.caa.multilogin.core.util.ValueUtil;
import moe.caa.multilogin.core.yggdrasil.YggdrasilService;
import moe.caa.multilogin.core.yggdrasil.YggdrasilServicesHandler;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class Verifier {
    public static final Set<User> CACHE_USER = new HashSet<>();
    public static final Map<UUID, String> CACHE_LOGIN = new HashMap<>();

    public static VerificationResult getUserVerificationMessage(UUID onlineUuid, String currentName, YggdrasilService yggdrasilService) {
        try {
            User userData = UserDataHandler.getUserEntryByOnlineUuid(onlineUuid);

            boolean updUserEntry = userData != null;

            // 验证服务器不符
            if (updUserEntry) {
                if (!Objects.equals(userData.yggdrasilService, yggdrasilService.path)) {
                    MultiLogger.log(LoggerLevel.DEBUG, LanguageKeys.VERIFICATION_NO_CHAE.getMessage(currentName, onlineUuid.toString(), yggdrasilService.name, yggdrasilService.path, userData.yggdrasilService));
                    return new VerificationResult(LanguageKeys.VERIFICATION_NO_CHAE.getMessage());
                }
            }

            //名称规范检查
            String reg = ValueUtil.notIsEmpty(yggdrasilService.nameAllowedRegular) ?
                    yggdrasilService.nameAllowedRegular : ValueUtil.notIsEmpty(MultiCore.nameAllowedRegular) ?
                    "" : MultiCore.nameAllowedRegular;

            if (!reg.isEmpty() && !Pattern.matches(reg, currentName)) {
                MultiLogger.log(LoggerLevel.DEBUG, LanguageKeys.DEBUG_VERIFICATION_NO_PAT_MATCH.getMessage(currentName, onlineUuid.toString(), reg));
                return new VerificationResult(LanguageKeys.VERIFICATION_NO_PAT_MATCH.getMessage());
            }

            //重名检查
            if (!MultiCore.safeId.contains(yggdrasilService.path)) {
                List<User> repeatedNameUserEntries = UserDataHandler.getUserEntryByCurrentName(currentName);
                for (User repeatedNameUserEntry : repeatedNameUserEntries) {
                    if (!repeatedNameUserEntry.equals(userData)) {
                        MultiLogger.log(LoggerLevel.DEBUG, LanguageKeys.DEBUG_VERIFICATION_RUSH_NAME.getMessage(currentName, onlineUuid.toString(), repeatedNameUserEntry.onlineUuid));
                        return new VerificationResult(LanguageKeys.VERIFICATION_RUSH_NAME.getMessage());
                    }
                }
            }

            userData = !updUserEntry ? new User(onlineUuid, currentName, yggdrasilService.convUuid.getResultUuid(onlineUuid, currentName), yggdrasilService.path, false) : userData;
            userData.currentName = currentName;

            if (!updUserEntry) {
                if (yggdrasilService.convRepeat) {
                    userData.redirectUuid = getRepeatUuid(userData, yggdrasilService);
                }
            }

            // 白名单检查
            if (!userData.whitelist && yggdrasilService.whitelist) {
                if (!(CacheWhitelistDataHandler.removeCacheWhitelist(currentName) | CacheWhitelistDataHandler.removeCacheWhitelist(onlineUuid.toString()))) {
                    MultiLogger.log(LoggerLevel.DEBUG, LanguageKeys.DEBUG_VERIFICATION_NO_WHITELIST.getMessage(currentName, onlineUuid.toString()));
                    return new VerificationResult(LanguageKeys.VERIFICATION_NO_WHITELIST.getMessage());
                }
                userData.whitelist = true;
            }

            if (updUserEntry) {
                UserDataHandler.updateUserEntry(userData);
            } else {
                UserDataHandler.writeNewUserEntry(userData);
            }

            // 重名踢出
            FutureTask<String> task = new FutureTask<>(() -> {
                for (ISender sender : MultiCore.plugin.getPlayer(currentName)) {
                    if (!sender.getPlayerUniqueIdentifier().equals(onlineUuid)) {
                        sender.kickPlayer(LanguageKeys.VERIFICATION_RUSH_NAME_ONL.getMessage());
                    }
                }
                return null;
            });

            // 等待主线程任务
            MultiCore.plugin.getSchedule().runTask(task);
            task.get();

            CACHE_LOGIN.put(userData.redirectUuid, currentName);
            CACHE_USER.add(userData);
            MultiLogger.log(LoggerLevel.INFO, LanguageKeys.VERIFICATION_ALLOW.getMessage(userData.onlineUuid.toString(), userData.currentName, userData.redirectUuid.toString(), yggdrasilService.name, yggdrasilService.path));
            return new VerificationResult(userData.redirectUuid, userData);
        } catch (Exception e) {
            MultiLogger.log(LoggerLevel.ERROR, e);
            MultiLogger.log(LoggerLevel.ERROR, LanguageKeys.VERIFICATION_ERROR.getMessage());
            return new VerificationResult(LanguageKeys.VERIFICATION_NO_ADAPTER.getMessage());
        }
    }

    private static UUID getRepeatUuid(User userData, YggdrasilService yggdrasilService) throws SQLException {
        UUID ret = userData.redirectUuid;
        if (UserDataHandler.getUserEntryByRedirectUuid(ret).size() == 0) {
            return ret;
        }
        MultiLogger.log(LoggerLevel.DEBUG, LanguageKeys.DEBUG_VERIFICATION_REPEAT_UUID.getMessage(
                userData.currentName,
                userData.onlineUuid,
                userData.redirectUuid.toString(),
                (userData.redirectUuid = UUID.randomUUID()).toString(),
                yggdrasilService.name,
                yggdrasilService.path
        ));
        return getRepeatUuid(userData, yggdrasilService);
    }

    /**
     * 按照玩家名字分批次排序 Yggdrasil 验证服务器
     *
     * @param name name
     * @return 验证服务器排序的结果
     */
    public static List<List<YggdrasilService>> getVeriOrder(String name) throws SQLException {
        List<List<YggdrasilService>> ret = new ArrayList<>();
        Set<YggdrasilService> one = UserDataHandler.getYggdrasilServiceByCurrentName(name);
        one.removeIf(yggdrasilService -> !yggdrasilService.enable);
        List<YggdrasilService> two = new ArrayList<>();
        for (YggdrasilService serviceEntry : YggdrasilServicesHandler.getServices()) {
            if (!serviceEntry.enable) continue;
            if (!one.isEmpty() && one.contains(serviceEntry)) continue;
            two.add(serviceEntry);
        }
        if (!one.isEmpty())
            ret.add(new ArrayList<>(one));
        ret.add(two);
        return ret;
    }
}
