import json
import unittest

import generate_icons as g

BUNDLE = g.BUNDLE.read_text()


class GenerateIconsTest(unittest.TestCase):
    def test_circle_becomes_two_arcs(self):
        self.assertEqual(g.circle('12', '12', '10'), 'M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0Z')

    def test_rounded_rectangle_retains_arcs(self):
        self.assertEqual(g.rect({'x': '3', 'y': '11', 'width': '18', 'height': '11', 'rx': '2', 'ry': '2'}),
                         'M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-14a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z')

    def test_unmatched_board_entry_names_the_board(self):
        with self.assertRaises(SystemExit) as raised:
            g.board_nodes('missing', {'board': 'SetHome', 'first': 'NO SUCH PATH'})
        self.assertIn('SetHome', str(raised.exception))
        self.assertIn('missing', str(raised.exception))

    def test_material_vectors_translate_the_negative_viewbox(self):
        text = g.material_render()
        self.assertIn('addGroup(translationY = 960f)', text)
        self.assertIn('960f, 960f', text)
        self.assertEqual(text, g.material_render())

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
        self.assertEqual(len(names), 37)
        text = g.generate()
        for name in names:
            self.assertIn(f'"{name}"', text)


if __name__ == '__main__':
    unittest.main()
