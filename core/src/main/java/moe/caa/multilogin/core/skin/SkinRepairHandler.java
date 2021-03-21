/*
 * Copyleft (c) 2021 ksqeib,CaaMoe. All rights reserved.
 * @author  ksqeib <ksqeib@dalao.ink> <https://github.com/ksqeib445>
 * @author  CaaMoe <miaolio@qq.com> <https://github.com/CaaMoe>
 * @github  https://github.com/CaaMoe/MultiLogin
 *
 * moe.caa.multilogin.core.skin.SkinRepairHandler
 *
 * Use of this source code is governed by the GPLv3 license that can be found via the following link.
 * https://github.com/CaaMoe/MultiLogin/blob/master/LICENSE
 */

package moe.caa.multilogin.core.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import moe.caa.multilogin.core.data.data.PluginData;
import moe.caa.multilogin.core.data.data.UserProperty;
import moe.caa.multilogin.core.data.databse.SQLHandler;
import moe.caa.multilogin.core.http.HttpGetter;

import java.util.UUID;

public class SkinRepairHandler {

    private static boolean repairThirdPartySkin(UserProperty property, UserProperty.Property onlineProperty) throws Exception {
        String skin = getSkinUrl(new String(onlineProperty.getDecoderValue()));

        if(PluginData.isEmpty(skin) || skin.contains("minecraft.net")) return false;

        // TODO: 2021/3/20 FAIL REQUEST : 500 
        String response = HttpGetter.httpPost("https://api.mineskin.org/generate/url", skin, 1);

        JsonObject value = new JsonParser().parse(response).getAsJsonObject();
        if(value.has("data")){
            JsonObject data = value.get("data").getAsJsonObject().get("texture").getAsJsonObject();
            if(!data.has("signature"))
                return false;
            property.setProperty(onlineProperty);
            property.setRepair_property(new UserProperty.Property(data.get("value").getAsString(),  data.get("signature").getAsString()));
            return true;
        }
        return false;
    }

    private static String getSkinUrl(String root){
        try {
            return new JsonParser().parse(root).getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
        } catch (Exception ignore){}
        return null;
    }

    public static UserProperty repairThirdPartySkin(UUID onlineUuid, String value, String signature) throws Exception {
        if(!PluginData.isOpenSkinRepair()) return new UserProperty(onlineUuid, null, new UserProperty.Property(value, signature));
        boolean newUserEntry;
        UserProperty userProperty = SQLHandler.getUserPropertyByOnlineUuid(onlineUuid);
        newUserEntry = userProperty == null;
        userProperty = userProperty == null ? new UserProperty(onlineUuid, new UserProperty.Property(), new UserProperty.Property()) : userProperty;

        repairThirdPartySkin(userProperty, new UserProperty.Property(value, signature));
        userProperty.setProperty(new UserProperty.Property(value, signature));
        if(newUserEntry){
            SQLHandler.writeNewUserProperty(userProperty);
        } else {
            SQLHandler.updateUserProperty(userProperty);
        }
        return userProperty;
    }
}
