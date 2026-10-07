package com.wind.meditor.visitor;

import com.wind.meditor.property.ModificationProperty;
import com.wind.meditor.utils.Log;
import com.wind.meditor.utils.NodeValue;
import com.wind.meditor.visitor.UserPermissionTagVisitor;
import java.util.ArrayList;
import java.util.List;
import pxb.android.axml.NodeVisitor;

public class ManifestTagVisitor extends ModifyAttributeVisitor {
    private UserPermissionTagVisitor.IUsesPermissionGetter addedPermissionGetter;
    private List<String> hasIncludedUsesPermissionList;
    private ModificationProperty properties;

    public ManifestTagVisitor(NodeVisitor nv, ModificationProperty properties) {
        super(nv, properties.getManifestAttributeList());
        this.hasIncludedUsesPermissionList = new ArrayList();
        this.properties = properties;
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        Log.d(" ManifestTagVisitor child  --> ns = " + ns + " name = " + name);
        if (ns != null && NodeValue.UsesPermission.TAG_NAME.equals(name)) {
            return new UserPermissionTagVisitor(super.child(null, NodeValue.UsesPermission.TAG_NAME), null, ns);
        }
        NodeVisitor child = super.child(ns, name);
        if (NodeValue.Application.TAG_NAME.equals(name)) {
            return new ApplicationTagVisitor(child, this.properties.getApplicationAttributeList(), this.properties.getMetaDataList(), this.properties.getDeleteMetaDataList());
        }
        if (NodeValue.UsesSDK.TAG_NAME.equals(name)) {
            return new ModifyAttributeVisitor(child, this.properties.getUsesSdkAttributeList());
        }
        return NodeValue.UsesPermission.TAG_NAME.equals(name) ? new UserPermissionTagVisitor(child, getUsesPermissionGetter(), null) : child;
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        Log.d(" ManifestTagVisitor attr  --> ns = " + ns + " name = " + name + " resourceId=" + resourceId + " obj = " + obj);
        super.attr(ns, name, resourceId, type, obj);
    }

    @Override
    public void end() {
        List<String> list = this.properties.getUsesPermissionList();
        if (list != null && list.size() > 0) {
            for (String permissionName : list) {
                if (!this.hasIncludedUsesPermissionList.contains(permissionName)) {
                    child(permissionName, NodeValue.UsesPermission.TAG_NAME);
                }
            }
        }
        super.end();
    }

    // ✅ FIXED: မှန်ကန်တဲ့ method name "onPermissionGetted" ကိုသုံးပါ
    private UserPermissionTagVisitor.IUsesPermissionGetter getUsesPermissionGetter() {
        if (this.addedPermissionGetter == null) {
            this.addedPermissionGetter = new UserPermissionTagVisitor.IUsesPermissionGetter() {
                @Override
                public void onPermissionGetted(String permissionName) {
                    if (!hasIncludedUsesPermissionList.contains(permissionName)) {
                        hasIncludedUsesPermissionList.add(permissionName);
                    }
                }
            };
        }
        return this.addedPermissionGetter;
    }
}