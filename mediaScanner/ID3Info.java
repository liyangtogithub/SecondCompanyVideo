package com.adayo.mediaScanner;

import android.os.Parcel;
import android.os.Parcelable;

public class ID3Info implements Parcelable{  
	public MediaObjectName file;
	public String title;
	public MediaObjectName artist;
	public MediaObjectName album;
	public MediaObjectName genre;
	public MediaObjectName composer;
	public String title_py;
	public String artist_py;
	public String album_py;
	public String genre_py;
	public String composer_py;
	public String duration;
	public String track_number;
	public String picPath;
	public ID3Info(){
		file=null;
		title=null;
		artist=null;
		album=null;
		genre=null;
		composer=null;
		duration=null;
		track_number=null;
		picPath=null;
	}
	
	public ID3Info(Parcel source){
		file=MediaObjectName.CREATOR.createFromParcel(source);
		title = source.readString();
		artist=MediaObjectName.CREATOR.createFromParcel(source);
		album=MediaObjectName.CREATOR.createFromParcel(source);
		genre=MediaObjectName.CREATOR.createFromParcel(source);
		composer=MediaObjectName.CREATOR.createFromParcel(source);
		duration = source.readString();
		track_number = source.readString();
		picPath = source.readString();
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		file.writeToParcel(arg0, arg1);
		arg0.writeString(title);
		artist.writeToParcel(arg0, arg1);
		album.writeToParcel(arg0, arg1);
		genre.writeToParcel(arg0, arg1);
		composer.writeToParcel(arg0, arg1);
		arg0.writeString(duration);
		arg0.writeString(track_number);
		arg0.writeString(picPath);
		
	}
	
	 public static final Parcelable.Creator<ID3Info> CREATOR = new Parcelable.Creator<ID3Info>() {

		@Override
		public ID3Info createFromParcel(Parcel arg0) {
			// TODO Auto-generated method stub
			return new ID3Info(arg0);
		}

		@Override
		public ID3Info[] newArray(int arg0) {
			// TODO Auto-generated method stub
			return new ID3Info[arg0];
		}  
		 
	 };
}
