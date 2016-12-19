package things.android.a.lightup;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * A simple program to turn on/off the LED connected to a GPIO port #12
 * on Intel Edison board and controlling button connected to GPIO port #13.
 *
 * The Intel Edison board is running Android Things Developer Preview.
 * Also, the button and a LED used for testing this program are from
 * Grove IoT Starter kit.
 *
 */
public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();

    /*
     * The GPIO pin number on the board where LED is connected.
     * In this case, LED is connected to PIN # 12 as per the board
     * layout numbering scheme.
     */
    private static final String GPIO_PIN_NAME_LED = "IO12";

    /*
     * The button is connected to a GPIO pin # 13.
     */
    private static final String GPIO_PIN_NAME_BUTTON = "IO13";

    /* Things object representing a LED connection */
    private Gpio mLed;

    /* Things object representing a Button connection */
    private Gpio mButton;

    /* LED operations handler */
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // PeripheralService enumerates the connection capabilities
        // of the board.
        PeripheralManagerService peripheralManagerService = new PeripheralManagerService();

        // Get the list of peripherals of GPIO types.
        if (peripheralManagerService.getGpioList().isEmpty()) {
            Log.e(TAG, "Board does not support GPIO connections");
            finish();
        }


        try {
            // Setup a button
            mButton = peripheralManagerService.openGpio(GPIO_PIN_NAME_BUTTON);
            mButton.setDirection(Gpio.DIRECTION_IN);

            // Setup a trigger type for this pin. This simply says
            // when should button be considered as "pressed".
            mButton.setEdgeTriggerType(Gpio.EDGE_FALLING);

            // Register a button callback
            mButton.registerGpioCallback(mButtonPressedCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error opening GPIO port " + GPIO_PIN_NAME_BUTTON);
            throw new RuntimeException(e);
        }

        try {
            // Setup a LED

            // Open a GPIO port IO12 and configure the port to be used
            // as a OUTPUT port.
            mLed = peripheralManagerService.openGpio(GPIO_PIN_NAME_LED);
            mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        } catch (IOException e) {
            Log.e(TAG, "Error opening GPIO port " + GPIO_PIN_NAME_LED);
            throw new RuntimeException(e);
        }

    }


    private GpioCallback mButtonPressedCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            mHandler.post(mBlinkerRunnable);
            return true;
        }
    };


    private Runnable mBlinkerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLed == null) {
                Log.e(TAG, "The LED connection instance is null");
                return;
            }

            try {
                mLed.setValue(!mLed.getValue());  // toggle
            } catch (IOException e) {
                Log.e(TAG, "LED Blinking error " + e.getMessage());
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release the resources.
        mHandler.removeCallbacks(mBlinkerRunnable);

        if (mButton != null) {
            mButton.unregisterGpioCallback(mButtonPressedCallback);

            try {
                mButton.close();
            } catch (IOException e) {
                Log.w(TAG, "Error occurred while closing the GPIO port " + GPIO_PIN_NAME_BUTTON);
            }
        }


        if (mLed != null) {
            try {
                mLed.close();
            } catch (IOException e) {
                Log.w(TAG, "Error occurred while closing the GPIO port " + GPIO_PIN_NAME_LED);
            }
        }
    }
}
