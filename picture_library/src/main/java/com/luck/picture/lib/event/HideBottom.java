package com.luck.picture.lib.event;

/**
 * description:
 * author:         xp
 * createDate:     2021/5/11 下午 6:23
 */
public class HideBottom {
    private boolean hide;

    public HideBottom(boolean hide) {
        this.hide = hide;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }
}
