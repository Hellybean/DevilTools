/**
 * DevilPreference.java
 * Jan 14, 2012 9:36:34 AM
 */
package mobi.cyann.deviltools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.cyann.deviltools.PreferenceListFragment.OnPreferenceAttachedListener;
import mobi.cyann.deviltools.preference.IntegerPreference;
import mobi.cyann.deviltools.SysCommand;
import mobi.cyann.deviltools.preference.CustomListPreference;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
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
public class PerformanceFragment extends BasePreferenceFragment {
	public PerformanceFragment() {
		super(R.layout.performance);
	}
    private ContentResolver mContentResolver;
    private static SharedPreferences preferences;

    private static final String GPU_CONTROL = "key_gpu_control";
    private static final String CATEGORY_GPU_CONTROL = "key_gpu_control_category";

    private PreferenceScreen mGpuOc;
    private CustomListPreference mGpuClock[] = new CustomListPreference[5];
    private CustomListPreference step0_clk;
    private CustomListPreference step1_clk;
    private CustomListPreference step2_clk;
    private CustomListPreference step3_clk;
    private CustomListPreference step4_clk;

    public static final String[] GPU_CLOCK_FILE_PATH = new String[] {
	"/sys/module/mali/parameters/step0_clk",
	"/sys/module/mali/parameters/step1_clk",
	"/sys/module/mali/parameters/step2_clk",
	"/sys/module/mali/parameters/step3_clk",
	"/sys/module/mali/parameters/step4_clk"
	};

    private static final String[] KEY_GPU_CLOCK = new String[] {
        "key_step0_clk",
        "key_step1_clk",
        "key_step2_clk",
        "key_step3_clk",
        "key_step4_clk",
    };

    public static final String[] GPU_THRESHOLD_FILE_PATH = new String[] {
	"/sys/module/mali/parameters/step0_up",
	"/sys/module/mali/parameters/step1_up",
	"/sys/module/mali/parameters/step2_up",
	"/sys/module/mali/parameters/step3_up",
	"/sys/module/mali/parameters/step1_down",
	"/sys/module/mali/parameters/step2_down",
	"/sys/module/mali/parameters/step3_down",
	"/sys/module/mali/parameters/step4_down",
	};

    private static final String GPU_AVAILABLE_FREQ = "/sys/devices/virtual/misc/gpu_control/available_frequencies";


	@Override
    	public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);

	preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        PreferenceScreen prefSet = getPreferenceScreen();
        mContentResolver = getActivity().getApplicationContext().getContentResolver();
	SysCommand sysCommand = SysCommand.getInstance();
	mGpuOc = (PreferenceScreen) prefSet.findPreference(GPU_CONTROL);
    	final PreferenceCategory gpucontrolCategory =
                (PreferenceCategory) prefSet.findPreference(CATEGORY_GPU_CONTROL);

	step0_clk = (CustomListPreference)findPreference(getString(R.string.key_step0_clk));
	step1_clk = (CustomListPreference)findPreference(getString(R.string.key_step1_clk));
	step2_clk = (CustomListPreference)findPreference(getString(R.string.key_step2_clk));
	step3_clk = (CustomListPreference)findPreference(getString(R.string.key_step3_clk));
	step4_clk = (CustomListPreference)findPreference(getString(R.string.key_step4_clk));
        if (Utils.fileExists(GPU_AVAILABLE_FREQ)) {
	reloadFrequencies();
	}
    }

	private Integer[] readAvailableFrequencies() {
		SysCommand sc = SysCommand.getInstance();
		Integer availableFreqs[] = null;
		int n = sc.readSysfs("/sys/devices/virtual/misc/gpu_control/available_frequencies");
		if(n > 0) {
			String temp = sc.getLastResult(0);
			String f[] = temp.split(" ");
			availableFreqs = new Integer[f.length];
			for(int i = 0; i < f.length; ++i) {
				availableFreqs[i] = Integer.parseInt(f[i]);
			}
		}
		return availableFreqs;
	}

	private void reloadFrequencies() {
			Integer availableFreqs[] = readAvailableFrequencies();
			String availableFreqsStr[] = new String[availableFreqs.length];
			for(int i = 0; i < availableFreqs.length; ++i) {
				availableFreqsStr[i] = availableFreqs[i] + " MHz";
			}
		if(step0_clk != null) {	
			step0_clk.setListValues(availableFreqs);
			step0_clk.setListLabels(availableFreqsStr);
			step0_clk.reload(false);
		}
		if(step1_clk != null) {	
			step1_clk.setListValues(availableFreqs);
			step1_clk.setListLabels(availableFreqsStr);
			step1_clk.reload(false);
		}
		if(step2_clk != null) {	
			step2_clk.setListValues(availableFreqs);
			step2_clk.setListLabels(availableFreqsStr);
			step2_clk.reload(false);
		}
		if(step3_clk != null) {	
			step3_clk.setListValues(availableFreqs);
			step3_clk.setListLabels(availableFreqsStr);
			step3_clk.reload(false);
		}
		if(step4_clk != null) {	
			step4_clk.setListValues(availableFreqs);
			step4_clk.setListLabels(availableFreqsStr);
			step4_clk.reload(false);
		}
	}

    public static void setPreferenceString(String key, String value) {
	Editor ed = preferences.edit();
	ed.putString(key, value);
	ed.commit();
    }

    public static void setPreferenceInteger(String key, int value) {
	Editor ed = preferences.edit();
	ed.putInt(key, value);
	ed.commit();
    }

    public static boolean ocIsSupported() {
        boolean exists = true;
        for (String filePath : GPU_CLOCK_FILE_PATH) {
            if (!Utils.fileExists(filePath)) {
                exists = false;
            }
        }
	return exists;
   }

    public static boolean thresholdIsSupported() {
        boolean exists = false;
        for (String filePath : GPU_THRESHOLD_FILE_PATH) {
            if (Utils.fileExists(filePath)) {
                exists = true;
            }
        }
	return exists;
   }

    public static boolean IsSupported() {
	    boolean ocsupported = ocIsSupported();
	    boolean thresholdsupported = thresholdIsSupported();
	    return (ocsupported || thresholdsupported);
    }
}
