package com.example.qqprivatenotify;
import android.content.*; import android.database.*; import android.net.Uri;
public final class ConfigProvider extends ContentProvider {
    static final String PREFS="settings", KEY_ENABLED="whitelist_mode", KEY_PRIVATE="private_ids", KEY_GROUP="group_ids", KEY_PRIVATE_MODE="private_whitelist", KEY_GROUP_MODE="group_whitelist";
    @Override public boolean onCreate(){return true;}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String sort){
        if(getContext()==null)return null; MatrixCursor c=new MatrixCursor(new String[]{KEY_ENABLED,KEY_PRIVATE,KEY_GROUP,KEY_PRIVATE_MODE,KEY_GROUP_MODE,WhitelistConfig.PRIVATE_ENABLED,WhitelistConfig.GROUP_ENABLED});
        SharedPreferences x=getContext().getSharedPreferences(PREFS,0);
        boolean old=x.getBoolean(KEY_ENABLED,true); c.addRow(new Object[]{old?1:0,x.getString(KEY_PRIVATE,""),x.getString(KEY_GROUP,""),x.getBoolean(KEY_PRIVATE_MODE,old)?1:0,x.getBoolean(KEY_GROUP_MODE,old)?1:0,x.getBoolean(WhitelistConfig.PRIVATE_ENABLED,true)?1:0,x.getBoolean(WhitelistConfig.GROUP_ENABLED,false)?1:0}); return c;
    }
    @Override public String getType(Uri u){return "vnd.android.cursor.item/config";}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
