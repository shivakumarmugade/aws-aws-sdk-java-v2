#!bin/python3

import os
import plotly.graph_objects as go

# 8x6 matrices where each row is all the attempt for the same benchmark
data_single_file = {
    'copy': {
        'v1': [
            # 1B
            0.036, 0.04, 0.044, 0.036, 0.037, 0.036, 0.043, 0.037,
            # 8MB-1
            0.042, 0.039, 0.043, 0.05, 0.043, 0.049, 0.055, 0.045,
            # 8MB+1
            0.06, 0.06, 0.094, 0.076, 0.045, 0.077, 0.062, 0.071,
            # 128MB
            0.832, 0.869, 0.842, 0.739, 0.892, 0.784, 0.772, 0.904,
            # 4GB
            34.113, 37.754, 36.9, 36.369, 35.358, 34.21, 36.525, 44.225,
            # 30GB
            6.463, 5.726, 5.994, 9.275, 10.091, 5.152, 5.85, 6.524
        ],
        'v2': [
            1.053, 0.056, 0.043, 0.066, 0.056, 0.056, 0.045, 0.045,
            0.058, 0.062, 0.071, 0.069, 0.066, 0.066, 0.074, 0.166,
            0.095, 0.072, 0.088, 0.135, 0.088, 0.076, 0.068, 0.105,
            0.371, 1.886, 5.315, 0.4, 0.367, 2.364, 0.387, 0.42,
            3.565, 3.869, 3.789, 2.447, 5.423, 2.046, 5.478, 5.175,
            7.366, 7.946, 8.59, 8.243, 8.332, 7.607, 7.606, 8.041
        ],
    },
    'download': {
        'disk': {
            'v1': [
                0.025, 0.025, 0.024, 0.042, 0.026, 0.023, 0.024, 0.025,
                0.187, 0.117, 0.136, 0.113, 0.117, 0.116, 0.113, 0.114,
                0.306, 0.144, 0.248, 0.122, 0.116, 0.115, 0.113, 0.148,
                0.499, 0.344, 0.273, 0.286, 0.332, 0.447, 0.348, 0.29,
                3.139, 2.811, 2.571, 2.509, 2.724, 2.484, 2.522, 2.563,
                52.904, 52.058, 45.316, 44.583, 50.343, 50.369, 53.261, 47.356
            ],
            'v2': [
                0.031, 0.04, 0.036, 0.027, 0.037, 0.049, 0.042, 0.026,
                0.166, 0.13, 0.105, 0.122, 0.108, 0.118, 0.12, 0.111,
                0.201, 0.133, 0.233, 0.13, 0.122, 0.122, 0.117, 0.126,
                0.334, 0.241, 0.186, 0.184, 0.194, 0.239, 0.236, 0.191,
                1.628, 1.565, 1.367, 1.641, 1.488, 1.353, 1.423, 1.451,
                23.328, 25.317, 26.666, 25.147, 25.899, 25.879, 23.408, 26.406
            ]
        },
        'tmpfs': {
            'v1': [
                0.038, 0.034, 0.032, 0.034, 0.032, 0.031, 0.03, 0.044,
                0.178, 0.113, 0.113, 0.114, 0.114, 0.113, 0.121, 0.113,
                0.211, 0.135, 0.116, 0.114, 0.114, 0.114, 0.115, 0.113,
                0.455, 0.372, 0.278, 0.258, 0.258, 0.255, 0.275, 0.256,
                2.474, 2.392, 2.377, 2.472, 2.511, 2.554, 2.547, 2.579,
                45.586, 44.304, 43.423, 46.81, 51.413, 50.372, 49.704, 42.938
            ],
            'v2': [
                0.046, 0.021, 0.041, 0.024, 0.026, 0.149, 0.026, 0.026,
                0.178, 0.12, 0.123, 0.117, 0.125, 0.124, 0.122, 0.152,
                0.202, 0.198, 0.122, 0.127, 0.108, 0.109, 0.14, 0.12,
                0.352, 0.274, 0.183, 0.193, 0.202, 0.199, 0.191, 0.191,
                1.493, 1.373, 1.345, 1.459, 1.474, 1.453, 1.393, 1.353,
                17.945, 17.071, 16.921, 17.742, 17.338, 21.262, 20.168, 19.728
            ]
        }
    },
    'upload': {
        'disk': {
            'v1': [
                0.032, 0.02, 0.018, 0.018, 0.017, 0.018, 0.019, 0.018,
                0.227, 0.143, 0.146, 0.171, 0.165, 0.142, 0.139, 0.129,
                0.145, 0.15, 0.129, 0.149, 0.153, 0.15, 0.171, 0.126,
                0.398, 0.362, 0.318, 0.334, 0.351, 3.419, 0.339, 0.287,
                6.058, 6.073, 1.941, 1.943, 3.692, 5.452, 5.313, 5.935,
                10.684, 10.801, 10.221, 12.64, 11.778, 12.351, 10.516, 12.691,

            ],
            'v2': [
                0.02, 0.019, 0.029, 0.029, 0.035, 0.023, 0.03, 0.034,
                0.267, 0.417, 0.2, 0.287, 0.252, 0.253, 0.27, 0.29,
                0.218, 0.283, 0.294, 0.266, 0.359, 0.272, 0.318, 0.236,
                0.468, 0.487, 0.509, 0.559, 0.55, 0.497, 0.44, 0.476,
                4.037, 5.459, 5.771, 2.844, 2.987, 5.337, 2.614, 5.106,
                37.123, 37.304, 38.615, 32.718, 37.608, 40.833, 48.343, 39.442
            ]
        },
        'tmpfs': {
            'v1': [
                0.037, 0.018, 0.015, 0.02, 0.015, 0.021, 0.016, 0.02,
                0.25, 0.161, 0.172, 0.153, 0.602, 0.145, 0.221, 0.173,
                0.178, 0.161, 0.206, 0.156, 0.196, 0.157, 0.165, 0.171,
                0.476, 0.353, 0.355, 1.103, 0.325, 0.351, 0.324, 0.68,
                1.202, 3.178, 0.903, 4.687, 0.744, 2.987, 5.67, 0.91,
                11.233, 10.044, 8.174, 7.526, 9.358, 10.046, 9.606, 10.037
            ],
            'v2': [
                0.033, 0.031, 0.025, 0.034, 0.034, 0.03, 0.033, 0.041,
                0.267, 0.31, 0.316, 0.264, 0.302, 0.286, 0.269, 0.268,
                0.236, 0.293, 0.315, 0.293, 0.252, 0.266, 0.249, 0.317,
                0.582, 0.487, 0.547, 0.465, 0.46, 0.445, 0.621, 0.468,
                2.394, 2.434, 2.476, 4.944, 3.26, 2.454, 2.468, 2.397,
                33.261, 41.114, 33.014, 32.97, 34.138, 33.972, 33.001, 34.12
            ]
        }
    }
}

# 8x3 matrices where each row is all the attempt for the same benchmark
data_directory = {
    'download': {
        "disk": {
            "v1": [
                # 1Bx1000
                24.68, 20.315, 19.796, 17.808, 16.791, 18.575, 19.795, 19.865,
                # 4Kx1000
                20.448, 19.639, 17.199, 19.143, 20.359, 19.607, 20.298, 19.523,
                # 16Mx1000
                15.236, 15.127, 15.62, 16.003, 16.594, 17.597, 17.261, 17.779
            ],
            "v2": [
                0.624, 0.427, 0.435, 0.449, 0.416, 0.414, 0.517, 0.429,
                0.617, 0.48, 0.519, 0.432, 0.403, 0.411, 0.463, 0.402,
                3.291, 2.619, 2.698, 2.488, 2.618, 2.473, 2.428, 2.487
            ]
        },
        "tmpfs": {
            "v1": [
                19.468, 19.334, 19.428, 18.189, 17.86, 17.717, 17.384, 18.513,
                22.286, 18.909, 19.058, 19.645, 19.646, 20.196, 19.321, 19.678,
                17.807, 16.571, 16.324, 15.872, 16.626, 16.139, 16.437, 16.003
            ],
            "v2": [
                0.607, 0.474, 0.432, 0.392, 0.427, 0.359, 0.357, 0.323,
                0.633, 0.447, 0.476, 0.429, 0.398, 0.413, 0.476, 0.381,
                3.028, 2.692, 2.667, 2.543, 2.632, 2.578, 2.585, 2.492
            ]
        }
    },
    'upload': {
        "disk": {
            "v1": [
                0.341, 0.448, 0.487, 0.947, 0.304, 0.261, 0.297, 0.301,
                3.949, 0.392, 3.71, 1.005, 0.371, 0.354, 0.389, 0.371,
                8.009, 6.948, 7.37, 5.141, 3.546, 4.866, 4.325, 5.609
            ],
            "v2": [
                4.057, 3.504, 3.361, 3.363, 3.337, 3.319, 3.334, 3.305,
                4.124, 3.671, 3.582, 3.655, 3.481, 3.659, 3.434, 3.463,
                5.349, 9.489, 7.626, 7.471, 9.632, 7.452, 7.21, 7.082
            ]
        },
        "tmpfs": {
            "v1": [
                0.274, 0.212, 0.224, 0.24, 0.244, 0.222, 0.216, 0.239,
                0.384, 0.364, 0.363, 0.329, 0.349, 0.411, 0.392, 0.347,
                6.916, 6.307, 6.492, 8.0, 7.968, 7.322, 4.969, 6.121
            ],
            "v2": [
                3.547, 3.212, 3.191, 3.068, 3.036, 3.015, 3.116, 3.069,
                4.091, 3.563, 3.495, 3.435, 3.468, 3.434, 3.479, 3.408,
                5.531, 6.018, 5.639, 8.545, 5.927, 8.506, 6.945, 8.973
            ]
        }
    }
}

directory_sizes = ["1Bx1000", "1Bx1000", "1Bx1000", "1Bx1000", "1Bx1000", "1Bx1000", "1Bx1000", "1Bx1000",
                   "4Kx1000", "4Kx1000", "4Kx1000", "4Kx1000", "4Kx1000", "4Kx1000", "4Kx1000", "4Kx1000",
                   "16Mx1000", "16Mx1000", "16Mx1000", "16Mx1000", "16Mx1000", "16Mx1000", "16Mx1000", "16Mx1000",
                   ]

single_file_sizes = ["1B", "1B", "1B", "1B", "1B", "1B", "1B", "1B",
                     "8MB-1", "8MB-1", "8MB-1", "8MB-1", "8MB-1", "8MB-1", "8MB-1", "8MB-1",
                     "8MB+1", "8MB+1", "8MB+1", "8MB+1", "8MB+1", "8MB+1", "8MB+1", "8MB+1",
                     "128MB", "128MB", "128MB", "128MB", "128MB", "128MB", "128MB", "128MB",
                     "4GB", "4GB", "4GB", "4GB", "4GB", "4GB", "4GB", "4GB",
                     "30GB", "30GB", "30GB", "30GB", "30GB", "30GB", "30GB", "30GB",
                     ]

v1_color = 'rgb(116, 116, 116)'
v2_color = 'rgb(21, 21, 21)'


def bar_graph(name, sizes, data, use_log=True):
    figure = go.Figure()
    figure.add_trace(go.Box(
        y=data['v1'],
        x=sizes,
        name='v1',
        marker_color=v1_color,
        boxpoints=False,
        boxmean=True,
        line_width=0.8
    ))
    figure.add_trace(go.Box(
        y=data['v2'],
        x=sizes,
        name='v2',
        marker_color=v2_color,
        boxmean=True,
        boxpoints=False,
        line_width=0.8
    ))
    if use_log:
        figure.update_yaxes(type="log")
    figure.update_layout(
        title=name,
        yaxis_title='seconds',
        boxmode='group'
    )
    return figure

if not os.path.exists("../images"):
    os.mkdir("../images")

bar_graph("upload-tmpfs", single_file_sizes, data_single_file['upload']['tmpfs']).write_image("../images/upload-tmpfs.png")
bar_graph("upload-disk", single_file_sizes, data_single_file['upload']['disk']).write_image("../images/upload-disk.png")
bar_graph("download-tmpfs", single_file_sizes, data_single_file['download']['tmpfs']).write_image("../images/download-tmpfs.png")
bar_graph("download-disk", single_file_sizes, data_single_file['download']['disk']).write_image("../images/download-disk.png")
bar_graph("copy", single_file_sizes, data_single_file['copy']).write_image("../images/copy.png")

bar_graph("download-directory-tmpfs", directory_sizes, data_directory['download']['tmpfs']).write_image("../images/download-directory-tmpfs.png")
bar_graph("download-directory-disk", directory_sizes, data_directory['download']['disk']).write_image("../images/download-directory-disk.png")
bar_graph("upload-directory-tmpfs", directory_sizes, data_directory['upload']['tmpfs']).write_image("../images/upload-directory-tmpfs.png")
bar_graph("upload-directory-disk", directory_sizes, data_directory['upload']['disk']).write_image("../images/upload-directory-disk.png")
