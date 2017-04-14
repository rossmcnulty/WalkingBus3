package ut.walkingbus;


import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.util.Log;

import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
/**
 * Created by Ross on 4/6/2017.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 20)
public class UIAutomatorTest {
    private UiDevice mDevice;

    @Before
    public void before() {
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        assertThat(mDevice, notNullValue());

        // Start from the home screen
        mDevice.pressHome();

    }

    @org.junit.Test
    public void test() throws InterruptedException {
        openApp("ut.walkingbus");

        UiObject2 parentSignInButton = waitForObject(By.res("ut.walkingbus:id/parent_button"));

        parentSignInButton.click();

        UiObject2 addChildButton = waitForObject(By.res("ut.walkingbus:id/fab"));

        UiObject2 drawerView = waitForObject(By.res("ut.walkingbus:id/drawer_layout"));
        Rect bounds = drawerView.getVisibleBounds(); // Returns the bounds of the view as (left, top, right, bottom)

        int left_x = 0;
        int center_y = bounds.centerY();
        // Now you have the center of the view. You can just slide by using drag()
        mDevice.drag(left_x, center_y, 5000, center_y, 200);
        // Where amount of pixels can be a variable set to a fraction of the screen width

        //takeScreenshot("screenshot-1.png");

        //editText.setText("123456");
        //UiObject2 protectObject = waitForObject(By.text("Submit"));
        //protectObject.click();

        //takeScreenshot("screenshot-2.png");

        Thread.sleep(10000);
    }

    private void openApp(String packageName) {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private void takeScreenshot(String name) {
        Log.d("TEST", "takeScreenshot");
        String dir = String.format("%s/%s", Environment.getExternalStorageDirectory().getPath(), "test-screenshots");
        File theDir = new File(dir);
        if (!theDir.exists()) {
            theDir.mkdir();
        }
        mDevice.takeScreenshot(new File(String.format("%s/%s", dir, name)));
    }

    private UiObject2 waitForObject(BySelector selector) throws InterruptedException {
        UiObject2 object = null;
        int timeout = 30000;
        int delay = 1000;
        long time = System.currentTimeMillis();
        while (object == null) {
            object = mDevice.findObject(selector);
            Thread.sleep(delay);
            if (System.currentTimeMillis() - timeout > time) {
                fail();
            }
        }
        return object;
    }
}
