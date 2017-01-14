package com.example.austinguo550.bluetoothrssireceiver;

import java.util.List;
import java.util.UUID;

/**
 * Created by austinguo550 on 1/12/17.
 */

public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}
