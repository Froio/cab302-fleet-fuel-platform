module com.fuelfleet.cab302fleetfuelplatform {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.fuelfleet.cab302fleetfuelplatform to javafx.fxml;
    exports com.fuelfleet.cab302fleetfuelplatform;
}