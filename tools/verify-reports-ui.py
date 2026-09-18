"""Run the reporting UI smoke check against a disposable synthetic database."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
wrapper = [str(root / "mvnw.cmd")] if os.name == "nt" else ["bash", str(root / "mvnw")]
with tempfile.TemporaryDirectory(prefix="fleet-report-smoke-") as temporary:
    demo = Path(temporary)
    classpath_file = demo / "classpath.txt"
    subprocess.run(wrapper + ["test-compile", "dependency:build-classpath",
        "-Dmdep.outputFile=" + str(classpath_file)], cwd=root, check=True)
    classpath = os.pathsep.join([str(root / "target/test-classes"),
        str(root / "target/classes"), classpath_file.read_text().strip()])
    output = root / "docs/verification"
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run(["java", "-Dfleet.disposableDemo=true", "-cp", classpath,
        "com.fuelfleet.cab302fleetfuelplatform.ReportsSmokeMain", str(output)], cwd=demo, check=True)
