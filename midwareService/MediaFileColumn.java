package com.adayo.midware.mpeg.db;

import com.adayo.midware.constant.MpegConstantsDef;

import android.net.Uri;
import android.provider.BaseColumns;

public class MediaFileColumn implements BaseColumns{

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ MpegConstantsDef.DVD_AUTHORITY);

		public static final String _Id = "_id";
		
		public static final String Index = "_id";

		public static final String ParentId = "parentid";

		public static final String Clips = "clips";

		public static final String isFile = "isfile";
		
		public static final String FileName = "filename";
		
		public static final String Song = "song";

		public static final String Artist = "artist";
		
		public static final String Album = "album";
		
		public static final String Types = "types";
		
		public static final String Sort_Order = "id ";
	
}
