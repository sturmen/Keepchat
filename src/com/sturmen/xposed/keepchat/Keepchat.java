package com.sturmen.xposed.keepchat;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Keepchat implements IXposedHookLoadPackage {

	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.snapchat.android"))
			return;
		else
			XposedBridge.log("we are in Snapchat!");

		findAndHookMethod("com.snapchat.android.model.ReceivedSnap", lpparam.classLoader, "getImageBitmap", Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Bitmap myImage = (Bitmap) param.getResult();
				XposedBridge.log("Bitmap loaded.");
				//we find the root of where we can write to
				String root = Environment.getExternalStorageDirectory().toString();
				//we then go into our directory
				File myDir = new File(root + "/keepchat");
				XposedBridge.log("Saving to directory " + myDir.toString());
				//if /keepchat doesn't exist, make it.
				if (myDir.mkdirs())
					XposedBridge.log("Directory " + myDir.toString() + " was created.");
				String fname = "Image-"+ System.currentTimeMillis() + ".jpg";
				XposedBridge.log("Saving with filename " + fname);
				//construct a File object
				File file = new File (myDir, fname);
				//make sure it doesn't exist (should never execute due to timestamp in name)
				if (file.exists ())
					if (file.delete())
						XposedBridge.log("File " + fname + " overwritten.");
				try {
					//open a new outputstream for writing
					FileOutputStream out = new FileOutputStream(file);
					//use built-in bitmap function to turn it into a jpeg and write it to the stream
					myImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
					//flush the stream to make sure it's done
					out.flush();
					//close it
					out.close();
					//construct a log entry
					CharSequence text = "Saved to " + myDir.toString() + "/" +  fname + " !";
					XposedBridge.log(text.toString());
					//get the original context to use for the toast
					Context context = (Context) param.args[0];
					//construct a toast notification telling the user it was successful
					Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
					//display the toast for the user
					toast.show();
					XposedBridge.log("Toast displayed successfully.");
				} catch (Exception e) {
					XposedBridge.log("Error occured while saving the file.");
					e.printStackTrace();
				}
				//return the original image to the original caller so the app can continue
				//TODO offer to return nag image so user has to go to sdcard to see snap and buy my app
			}
	});

}

}
