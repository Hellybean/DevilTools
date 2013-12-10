/**
 * DevilPreference.java
 * Jan 14, 2012 9:36:34 AM
 */
package mobi.cyann.deviltools;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.cyann.deviltools.PreferenceListFragment.OnPreferenceAttachedListener;
import mobi.cyann.deviltools.preference.IntegerPreference;
import mobi.cyann.deviltools.SysCommand;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;

/**
 * @DerTeufel1980
 *
 */
public class MemoryFragment extends BasePreferenceFragment implements OnPreferenceChangeListener {
	public MemoryFragment() {
		super(R.layout.memory);
	}
	
	private ListPreference mBigmem;
	private ListPreference mZram;
	private ListPreference mZswap;

        private ContentResolver mContentResolver;
	private static SharedPreferences preferences;

    	private static final String BIGMEM_FILE_PATH = "/sys/kernel/bigmem/enable";
    	public static final String ZSWAP_FILE_SIZE_PATH = "/sys/block/vnswap0/disksize";
    	public static final String[] ZRAM_FILE_SIZE_PATH = new String[] {
    	"/sys/block/zram0/disksize",
    	"/sys/block/zram1/disksize",
    	"/sys/block/zram2/disksize",
    	"/sys/block/zram3/disksize",
	};

    	public static final String[] ZRAM_FILE_RESET_PATH = new String[] {
    	"/sys/block/zram0/reset",
    	"/sys/block/zram1/reset",
    	"/sys/block/zram2/reset",
    	"/sys/block/zram3/reset",
	};

    	public static final String[] ZRAM_FILE_PATH = new String[] {
    	"/dev/block/zram0",
    	"/dev/block/zram1",
    	"/dev/block/zram2",
    	"/dev/block/zram3",
	};

    	public static final String ZSWAP_FILE_PATH = "/dev/block/vnswap0";

	@Override
    	public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);

	preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        PreferenceScreen prefSet = getPreferenceScreen();
        mContentResolver = getActivity().getApplicationContext().getContentResolver();
	SysCommand sc = SysCommand.getInstance();

	// get current available ram
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long totalMegs = getTotalMem() / 1048576L;

        mBigmem = (ListPreference) findPreference("key_bigmem");
	if (!Utils.fileExists(BIGMEM_FILE_PATH)) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().findPreference("bigmem_category");
            category.removePreference(mBigmem);
            getPreferenceScreen().removePreference(category);
	} else {
        mBigmem.setOnPreferenceChangeListener(this);
        mBigmem.setSummary(String.valueOf("Current Ram: " + totalMegs) + " MB");
	}

        mZram = (ListPreference) findPreference("key_zram");
 	Preference button = (Preference)findPreference("zram_apply");
	int num_devices = zram_num_devices();
	if (num_devices < 1) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().findPreference("zram_category");
            category.removePreference(mZram);
            category.removePreference(button);
            getPreferenceScreen().removePreference(category);
	} else {
        mZram.setOnPreferenceChangeListener(this);
   	if(button != null) 
   	{
        button.setOnPreferenceClickListener(new Preference.
		OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        zram_apply(true);   
                        return true;
                    }
                });     
    	}

	zram_apply(false);
	}

    	mZswap = (ListPreference) findPreference("key_zswap");
 	Preference button_zswap = (Preference)findPreference("zswap_apply");
	if (!zswap_supported()) {
            PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().findPreference("zswap_category");
            category.removePreference(mZswap);
            category.removePreference(button_zswap);
            getPreferenceScreen().removePreference(category);
	} else {
        mZswap.setOnPreferenceChangeListener(this);
   	if(button_zswap != null) 
   	{
        button_zswap.setOnPreferenceClickListener(new Preference.
		OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        zswap_apply(true);   
                        return true;
                    }
                });     
    	}

	zswap_apply(false);
	}
    }
	
    public boolean onPreferenceChange(Preference preference, Object objValue) {
	int index;
	SysCommand sc = SysCommand.getInstance();
	if (preference == mBigmem) {
            index = mBigmem.findIndexOfValue((String) objValue);
            mBigmem.setSummary("After Reboot: " + mBigmem.getEntries()[index]);
	    sc.writeSysfs("/sys/kernel/bigmem/enable", ((String) objValue));
	    sc.writeSysfs("/data/local/devil/bigmem", ((String) objValue));
	    setPreferenceString(getString(R.string.key_bigmem), ((String) objValue));
            return true;
        } else if (preference == mZram) {
            String zramPercent = ((String) objValue);
	    setPreferenceString(getString(R.string.key_zram), zramPercent);
	    zram_apply(false);
            return true;
        } else if (preference == mZswap) {
            String zswapPercent = ((String) objValue);
	    setPreferenceString(getString(R.string.key_zswap), zswapPercent);
	    zswap_apply(false);
            return true;
        }
        return false;
    }

    private static void setPreferenceString(String key, String value) {
	Editor ed = preferences.edit();
	ed.putString(key, value);
	ed.commit();
    }

    private static void setPreferenceInteger(String key, int value) {
	Editor ed = preferences.edit();
	ed.putInt(key, value);
	ed.commit();
    }

    public static int zram_num_devices() {
        int count = 0;
        for (String filePath : ZRAM_FILE_SIZE_PATH) {
            if (Utils.fileExists(filePath)) {
                count++;
            }
        }
	return count;
    }

    public static boolean zswap_supported() {
        if (Utils.fileExists(ZSWAP_FILE_SIZE_PATH)) {
        	return true;
        }
	return false;
    }

    public static synchronized int readTotalRam() { 
 		int tm=1000; 
 		try { 
 			RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r"); 
 			String load = reader.readLine(); 
 			String[] totrm = load.split(" kB"); 
 			String[] trm = totrm[0].split(" "); 
 			tm=Integer.parseInt(trm[trm.length-1]); 
 			tm=Math.round(tm/1024); } 
 		catch (IOException ex) 
 		{ 
 			ex.printStackTrace(); } 
 		return tm; 
 	}
    
    @SuppressLint("NewApi")
	private long getTotalMem() {
    	MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long totalMem=0;
        if (Build.VERSION.SDK_INT >= 16)
        	totalMem=mi.totalMem;
        else
        	totalMem=readTotalRam();
		return totalMem;
    	
    }
    
    private long totalZramSize(String percent) {
    	String zramPercent = percent;       
    	long totalzramSize = (getTotalMem() / 100) * Integer.parseInt(zramPercent) / 1048576L;
    	return totalzramSize;
    }

    private long totalZswapSize(String percent) {
    	String zswapPercent = percent;       
    	long totalzswapSize = (getTotalMem() / 100) * Integer.parseInt(zswapPercent) / 1048576L;
    	return totalzswapSize;
    }

    
    
    private String zramCommand(String percent) {
	int num_devices = zram_num_devices();
	String zramPercent = percent;
	long zramSize = (getTotalMem() / num_devices / 100) * Integer.parseInt(zramPercent);
	setPreferenceString("zramSize", String.valueOf(zramSize));
	StringBuilder command = new StringBuilder();
        for (int i = 0; i < num_devices; i++) {
		command.append("swapoff " + ZRAM_FILE_PATH[i] + "\n");
		command.append("echo " + 1 + " > " + ZRAM_FILE_RESET_PATH[i] + "\n");
		command.append("echo " + String.valueOf(zramSize) + " > " + ZRAM_FILE_SIZE_PATH[i] + "\n");
		command.append("mkswap " + ZRAM_FILE_PATH[i] + "\n");
		if (zramSize!=0)
			command.append("swapon " + ZRAM_FILE_PATH[i] + "\n");
        }
	return command.toString();
    }

    private void zram_apply(boolean apply) {
	SysCommand sc = SysCommand.getInstance();
	String zramPercent = preferences.getString("key_zram", "0");
        mZram.setSummary(zramPercent + "%% of total availbale Ram are going to be used for compressed Memory" + "\n" + "This is equivalent to " + String.valueOf(totalZramSize(zramPercent)) + " MB");
	if (apply) {
	String command = zramCommand(zramPercent);
	sc.suRun(command);
	}
    }


    private String zswapCommand(String percent) {
	String zswapPercent = percent;
	long zswapSize = (getTotalMem() / 100) * Integer.parseInt(zswapPercent);
	setPreferenceString("zswapSize", String.valueOf(zswapSize));
	StringBuilder command = new StringBuilder();
		command.append("swapoff " + ZSWAP_FILE_PATH + "\n");
		command.append("echo " + String.valueOf(zswapSize) + " > " + ZSWAP_FILE_SIZE_PATH + "\n");
		command.append("mkswap " + ZSWAP_FILE_PATH + "\n");
		if (zswapSize!=0)
			command.append("swapon " + ZSWAP_FILE_PATH + "\n");
	return command.toString();
    }

    private void zswap_apply(boolean apply) {
	SysCommand sc = SysCommand.getInstance();
	String zswapPercent = preferences.getString("key_zswap", "0");
        mZswap.setSummary(zswapPercent + "%% of total availbale Ram are going to be used for Zswap" + "\n" + "This is equivalent to " + String.valueOf(totalZswapSize(zswapPercent)) + " MB");
	if (apply) {
	String command = zswapCommand(zswapPercent);
	sc.suRun(command);
	}
    }

}
