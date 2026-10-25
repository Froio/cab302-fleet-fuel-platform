"""Run the opt-in JavaFX application smoke test in a disposable directory."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
wrapper = [str(root / "mvnw.cmd")] if os.name == "nt" else ["bash", str(root / "mvnw")]
java_home = os.environ.get("JAVA_HOME")
java = str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if java_home else "java"
with tempfile.TemporaryDirectory(prefix="fleet-application-smoke-") as temporary:
    scratch = Path(temporary)
    classpath_file = scratch / "classpath.txt"
    subprocess.run(wrapper + ["test-compile", "dependency:build-classpath",
                   "-Dmdep.outputFile=" + str(classpath_file)], cwd=root, check=True)
    classpath = os.pathsep.join([str(root / "target/test-classes"), str(root / "target/classes"),
                               classpath_file.read_text().strip()])
    subprocess.run([java, "-Dfleet.disposableDemo=true", "-Djavafx.cachedir=" + str(scratch / "javafx-cache"),
                    "-cp", classpath, "com.fuelfleet.cab302fleetfuelplatform.ApplicationUiSmokeMain"],
                   cwd=scratch, check=True)
