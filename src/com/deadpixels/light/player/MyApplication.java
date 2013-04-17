package com.deadpixels.light.player;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import com.lazybitz.beta.light.player.R;

@ReportsCrashes(formKey = "dGNvR1o5cUI0WjcxWW5LcTRxVnBiZ1E6MQ", 
mode = ReportingInteractionMode.NOTIFICATION,
mailTo = "everest.deth@gmail.com",
resToastText  = R.string.app_crashed_prompt,
resNotifTickerText = R.string.app_crashed,
resNotifTitle = R.string.app_crashed,
resNotifText = R.string.app_crashed,
resDialogText = R.string.app_crashed_prompt
)
public class MyApplication extends Application{
	
	@Override
	  public void onCreate() {
	      super.onCreate();

	      // The following line triggers the initialization of ACRA
	      ACRA.init(this);
	  }
}
