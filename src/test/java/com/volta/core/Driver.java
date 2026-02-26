package com.volta.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.time.Duration;

/**
 * This class provides utility methods for managing web drivers.
 */
public class Driver {

    //create a private constructor to remove access to this object
    private Driver() {
    }

    /*
    We make the WebDriver private, because we want to close access from outside the class.
    We are making it static, because we will use it in a static method.   */
    // private static WebDriver driver; // default value = null /* It was our preview method to get a driver Now we will use a driverPool*/
    private static InheritableThreadLocal<WebDriver> driverPool = new InheritableThreadLocal<>();

    /*
    Create a re-usable utility method which will return the same driver instance once we call it.
    - If an instance doesn't exist, it will create first, and then it will always return the same instance.  */
    public static WebDriver getDriver() {

        // if(driver == null){  // It was our preview method to get a driver
        if (driverPool.get() == null) {

            /*
            We will read our browserType from the configuration.properties file.
            This way, we can control which browser is opened from outside our code.   */
            String browserType = ConfigurationReaderOutdated.getProperty("browser");

            // My addition to have default browser just in case of getProperty("browser"); is empty!
            if (browserType == null) {
                browserType = "chrome";
            }

            /*
            Depending on the browserType returned from the configuration.properties
            switch statement will determine the "case" and open the matching browser.  */
            switch (browserType.toLowerCase()) {


                case "firefox":
                    driverPool.set(new FirefoxDriver());
                    break;
                case "edge":
                    driverPool.set(new EdgeDriver());
                    break;
                case "headless-chrome":
                    ChromeOptions option = new ChromeOptions();
                    option.addArguments("--headless=new");
                    driverPool.set(new ChromeDriver(option));
                    break;

                case "chrome":
                default:
                    driverPool.set(new ChromeDriver());
                    break;
            }

            driverPool.get().manage().window().maximize();
            driverPool.get().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(getImplicitWait()));

        }
        return driverPool.get();
    }

    /* Create a new Driver.closeDriver(); it will use .quit() method to quit browsers, and then set the driver value back to null. */
    public static void closeDriver() {
        if (driverPool.get() != null) {
            /* This line will terminate the currently existing driver completely. It will not exist going forward. */
            driverPool.get().quit();
            /* We assign the value back to "null" so that my "singleton" can create a newer one if needed. */
            driverPool.remove();
        }
    }

    private static long getImplicitWait() {
        String wait = ConfigurationReaderOutdated.getProperty("implicitWait");
        return (wait == null || wait.isBlank()) ? 10 : Long.parseLong(wait);
    }


}
