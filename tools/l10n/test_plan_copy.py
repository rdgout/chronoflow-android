import contextlib
import hashlib
import io
import tempfile
import unittest
from pathlib import Path

import plan_copy as pc

SAMPLE = b'{\r\n  "entries": []\r\n}\r\n\xc3\xa9\xe2\x80\xa6'
PIN = "0" * 40


def run(tmp, *extra, data=SAMPLE):
    err = io.StringIO()
    with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
        code = pc.main(["--root", tmp, "--pin", PIN, *extra], read=lambda ios, pin: data)
    return code, err.getvalue()


class PlanCopyTest(unittest.TestCase):
    def test_copy_is_byte_identical(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(run(tmp)[0], 0)
            self.assertEqual((Path(tmp) / pc.TARGET).read_bytes(), SAMPLE)
            line = (Path(tmp) / pc.HASH_FILE).read_text()
            self.assertEqual(line, f"{hashlib.sha256(SAMPLE).hexdigest()}  {pc.TARGET}\n")
            self.assertEqual(run(tmp, "--check")[0], 0)

    def test_check_fails_on_edited_copy(self):
        with tempfile.TemporaryDirectory() as tmp:
            run(tmp)
            target = Path(tmp) / pc.TARGET
            target.write_bytes(SAMPLE + b" ")
            code, err = run(tmp, "--check")
            self.assertEqual(code, 1)
            self.assertIn("committed " + pc.TARGET, err)
            self.assertIn(hashlib.sha256(SAMPLE + b" ").hexdigest(), err)

    def test_check_fails_when_pin_hash_differs(self):
        with tempfile.TemporaryDirectory() as tmp:
            run(tmp)
            code, err = run(tmp, "--check", data=SAMPLE + b"x")
            self.assertEqual(code, 1)
            self.assertIn("iOS " + pc.SOURCE, err)

    def test_missing_pin_exits_2(self):
        def read(ios, pin):
            raise pc.PinNotFound(f"iOS pin {pin} not found in {ios}")

        with tempfile.TemporaryDirectory() as tmp, contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(pc.main(["--root", tmp, "--pin", PIN], read=read), 2)
            self.assertEqual(list(Path(tmp).rglob("*")), [])


if __name__ == "__main__":
    unittest.main()
