package guriguri.mond.util;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by guriguri on 2015. 1. 5..
 */
public class PropUtil {
    private static final Logger log = LoggerFactory.getLogger(PropUtil.class);

    public static final String FILE_PRIORITY_PROPERTY = "/sdcard/mond.properties";
    public static final String FILE_DEFAULT_PROPERTY = "mond.properties";

    public static final String FLAG_ON = "1";
    public static final String FLAG_OFF = "0";

    public static boolean isExist(String path) {
        File f = new File(path);

        if (f.isFile()) {
            return true;
        }

        return false;
    }

    public static Properties getProperties(InputStream is) {
        Properties props = new Properties();

        try {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        return props;
    }

    public static Properties getProperties(String path) {
        Properties props = new Properties();

        try {
            if (isExist(path) == true) {
                FileInputStream inputStream = new FileInputStream(path);
                props.load(inputStream);
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        return props;
    }

    public static void setProperties(String path, Properties props,
                                     String commit) throws Exception {
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            props.store(outputStream, commit);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
            throw e;
        }
    }

    public static boolean checkProperty(Properties props, String[] keys) {
        for (String key : keys) {
            String value = props.getProperty(key);
            if (StringUtils.isEmpty(value) == true) {
                log.error("ENV, {} is null or empty", key);
                return false;
            } else {
                log.info("ENV, {}={}", key, value);
            }
        }

        return true;
    }

    public static Properties getMergeProperty(Context context, String assetFile, String srcFile) {
        Properties baseProps = null;

        try {
            InputStream is = context.getAssets().open(assetFile);
            baseProps = getProperties(is);
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
            baseProps = new Properties();
        }

        Properties srcProps = getProperties(srcFile);

        for (Object obj : srcProps.keySet()) {
            String key = obj.toString();
            baseProps.setProperty(key, srcProps.getProperty(key));
        }

        return baseProps;
    }

    public static Integer getInt(Object obj) {
        Integer ret = null;

        try {
            if (obj == null) {
                /* nothing todo */
            } else if (obj instanceof Integer) {
                ret = (Integer) obj;
            } else if ((obj instanceof String) && (StringUtils.isEmpty((String) obj) == false)) {
                ret = Integer.parseInt((String) obj);
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

        return ret;
    }

    public static Boolean getBoolean(Object obj) {
        return FLAG_ON.equals(obj);
    }
}
