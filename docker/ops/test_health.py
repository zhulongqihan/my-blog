import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("health", Path(__file__).with_name("myblog-health.py"))
health = importlib.util.module_from_spec(spec)
spec.loader.exec_module(health)


class CapacityAlarms(unittest.TestCase):
    def test_thresholds_and_recovery(self):
        normal = dict(disk_percent=78, inode_percent=10, mem_available_mib=800,
                      swap_in_kib_s=10, swap_out_kib_s=0)
        self.assertEqual([], health.assess(normal))
        self.assertEqual(["disk_capacity"], health.assess({**normal, "disk_percent": 85}))
        self.assertEqual(["inode_capacity"], health.assess({**normal, "inode_percent": 85}))
        self.assertEqual(["memory_pressure"], health.assess({**normal, "mem_available_mib": 255}))
        self.assertEqual(["active_swapping"], health.assess({**normal, "swap_out_kib_s": 4096}))
        # Existing swap occupancy alone is not a pressure alarm.
        self.assertEqual([], health.assess({**normal, "swap_used_mib": 1500}))


if __name__ == "__main__":
    unittest.main()
