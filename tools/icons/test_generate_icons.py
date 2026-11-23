import json
import unittest

import generate_icons as g

BUNDLE = g.BUNDLE.read_text()


class GenerateIconsTest(unittest.TestCase):
    def test_circle_becomes_two_arcs(self):
        self.assertEqual(g.circle('12', '12', '10'), 'M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0Z')

    def test_skip_forward_polygon_closes(self):
        paths = g.nodes(BUNDLE, 'skip-forward')
        self.assertTrue(paths[0][0].endswith('Z'))
        self.assertTrue(paths[0][0].startswith('M5 4 L15 12'))
        self.assertEqual(paths[1][0], 'M19 5L19 19')

    def test_moon_is_the_board_version(self):
        self.assertEqual(g.nodes(BUNDLE, 'moon'), [('M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z', False)])

    def test_two_runs_are_byte_identical(self):
        self.assertEqual(g.generate(), g.generate())

    def test_unknown_name_fails(self):
        with self.assertRaises(SystemExit) as raised:
            g.nodes(BUNDLE, 'no-such-glyph')
        self.assertIn('no-such-glyph', str(raised.exception))

    def test_manifest_names_all_generate(self):
        names = json.loads(g.MANIFEST.read_text())['glyphs']
        self.assertEqual(len(names), 16)
        text = g.generate()
        for name in names:
            self.assertIn(f'"{name}"', text)


if __name__ == '__main__':
    unittest.main()
