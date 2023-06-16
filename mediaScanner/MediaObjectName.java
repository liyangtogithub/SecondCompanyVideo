package com.adayo.mediaScanner;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaObjectName implements Parcelable{  
	private int mID;
	private String mName;
	public MediaObjectName(int id, String name){
		mID = id;
		mName = name;
	}
	public int getID(){
		return mID;
	}
	public String getName(){
		return mName;
	}
	
	public MediaObjectName(Parcel source){
		mID = source.readInt();
		mName = source.readString();
	}
	
	public void writeToParcel(Parcel reply, int parcelableWriteReturnValue) {
		reply.writeInt(mID);
		reply.writeString(mName);
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	 public static final Parcelable.Creator<MediaObjectName> CREATOR = new Parcelable.Creator<MediaObjectName>() {

		@Override
		public MediaObjectName createFromParcel(Parcel arg0) {
			// TODO Auto-generated method stub
			return new MediaObjectName(arg0);
		}

		@Override
		public MediaObjectName[] newArray(int arg0) {
			// TODO Auto-generated method stub
			return new MediaObjectName[arg0];
		}  
		 
	 };
}
