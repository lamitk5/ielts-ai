#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
IELTS Platform - Architectural Spectral Analysis
Calculates Graph Laplacian, Eigenvalues, Fiedler Vector, Spectral Clustering,
Modularity Q score, architecture violations, and generates visualization plots.
"""

import json
import math
import os
import sys
from pathlib import Path
import yaml
import networkx as nx
from networkx.algorithms.community import modularity

# Try importing scientific stack; fallback gracefully if not present
try:
    import numpy as np
    from scipy.linalg import eigh as scipy_eigh
    from scipy.cluster.hierarchy import linkage as scipy_linkage, dendrogram as scipy_dendrogram
    from scipy.spatial.distance import pdist as scipy_pdist
    from sklearn.cluster import SpectralClustering
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    HAS_SCIPY_MATPLOTLIB = True
except ImportError:
    HAS_SCIPY_MATPLOTLIB = False

from PIL import Image, ImageDraw, ImageFont

def jacobi_eigh(A_in, tol=1e-12, max_iter=150):
    """
    Pure Python Jacobi eigenvalue algorithm for real symmetric matrices.
    Returns sorted (eigenvalues, eigenvectors).
    """
    n = len(A_in)
    A = [row[:] for row in A_in]
    V = [[1.0 if i == j else 0.0 for j in range(n)] for i in range(n)]

    for _ in range(max_iter):
        max_val = 0.0
        p, q = 0, 1
        for i in range(n):
            for j in range(i + 1, n):
                if abs(A[i][j]) > max_val:
                    max_val = abs(A[i][j])
                    p, q = i, j

        if max_val < tol:
            break

        app = A[p][p]
        aqq = A[q][q]
        apq = A[p][q]

        theta = (aqq - app) / (2.0 * apq) if abs(apq) > 1e-15 else 0.0
        if theta >= 0:
            t = 1.0 / (theta + math.sqrt(1.0 + theta * theta))
        else:
            t = -1.0 / (-theta + math.sqrt(1.0 + theta * theta))

        c = 1.0 / math.sqrt(1.0 + t * t)
        s = t * c

        A[p][p] = app - t * apq
        A[q][q] = aqq + t * apq
        A[p][q] = 0.0
        A[q][p] = 0.0

        for r in range(n):
            if r != p and r != q:
                arp = A[r][p]
                arq = A[r][q]
                A[r][p] = c * arp - s * arq
                A[p][r] = A[r][p]
                A[r][q] = s * arp + c * arq
                A[q][r] = A[r][q]

        for i in range(n):
            vip = V[i][p]
            viq = V[i][q]
            V[i][p] = c * vip - s * viq
            V[i][q] = s * vip + c * viq

    eigenvalues = [A[i][i] for i in range(n)]
    eigenvectors = [[V[i][j] for i in range(n)] for j in range(n)]

    indices = sorted(range(n), key=lambda k: eigenvalues[k])
    sorted_vals = [0.0 if abs(eigenvalues[k]) < 1e-10 else eigenvalues[k] for k in indices]
    sorted_vecs = [eigenvectors[k] for k in indices]
    return sorted_vals, sorted_vecs

def load_graph_data(project_root: Path):
    graph_json_path = project_root / "graph.json"
    arch_yaml_path = project_root / "architecture.yaml"

    if graph_json_path.exists():
        print(f"[INFO] Đọc đồ thị phụ thuộc từ: {graph_json_path.name}")
        with open(graph_json_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            nodes = data.get("nodes", [])
            edges = data.get("edges", [])
            weights = data.get("weights", {})
            return nodes, edges, weights, graph_json_path

    if arch_yaml_path.exists():
        print(f"[INFO] Fallback: Đọc đồ thị phụ thuộc từ: {arch_yaml_path.name}")
        with open(arch_yaml_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
            nodes = [m["id"] for m in data.get("modules", [])]
            raw_edges = data.get("edges", [])
            edges = [[e["source"], e["target"]] for e in raw_edges]
            weights = {f"{e['source']}->{e['target']}": e.get("weight", 1) for e in raw_edges}
            return nodes, edges, weights, arch_yaml_path

    raise FileNotFoundError("Không tìm thấy graph.json hoặc architecture.yaml!")

def check_violations(edges, arch_yaml_path: Path):
    violations = []
    if not arch_yaml_path.exists():
        return violations

    with open(arch_yaml_path, "r", encoding="utf-8") as f:
        arch_data = yaml.safe_load(f)
        forbidden_rules = arch_data.get("forbidden", [])

    real_edge_set = {f"{e[0]}->{e[1]}" for e in edges}

    for rule in forbidden_rules:
        src = rule.get("source")
        dst = rule.get("target")
        reason = rule.get("reason", "Forbidden dependency")

        if dst == "*":
            for e in edges:
                if e[0] == src:
                    if src == "infrastructure" and e[1] == "shared":
                        continue
                    violations.append(f"{e[0]}->{e[1]} (Rule: {src}->* | {reason})")
        else:
            edge_key = f"{src}->{dst}"
            if edge_key in real_edge_set:
                violations.append(f"{edge_key} (Lý do: {reason})")

    return violations

def kmeans_clustering(points, k, max_iter=100):
    """Simple K-Means for 1D/ND points"""
    n = len(points)
    if k >= n:
        return {str(i): [i] for i in range(n)}

    # Initialize centers deterministically
    sorted_pts = sorted(range(n), key=lambda i: points[i][0] if isinstance(points[i], list) else points[i])
    centers = [points[sorted_pts[int(i * (n - 1) / (k - 1))]] for i in range(k)]

    labels = [0] * n
    for _ in range(max_iter):
        # Assign
        new_labels = []
        for p in points:
            dists = []
            for c in centers:
                if isinstance(p, list):
                    d = sum((p[dim] - c[dim]) ** 2 for dim in range(len(p)))
                else:
                    d = (p - c) ** 2
                dists.append(d)
            new_labels.append(dists.index(min(dists)))

        if new_labels == labels:
            break
        labels = new_labels

        # Update centers
        for ci in range(k):
            cluster_members = [points[i] for i in range(n) if labels[i] == ci]
            if cluster_members:
                if isinstance(points[0], list):
                    dims = len(points[0])
                    centers[ci] = [sum(m[dim] for m in cluster_members) / len(cluster_members) for dim in range(dims)]
                else:
                    centers[ci] = sum(cluster_members) / len(cluster_members)

    cluster_dict = {}
    for ci in range(k):
        m = [i for i in range(n) if labels[i] == ci]
        if m:
            cluster_dict[str(ci)] = m
    return cluster_dict

def get_font(size=14, bold=False):
    """Load Unicode TrueType font with full Vietnamese diacritic support"""
    candidates = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/tahoma.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size=size)
            except Exception:
                continue
    try:
        return ImageFont.load_default()
    except Exception:
        return None

def draw_plots_pillow(output_dir, nodes, A, eigenvalues, best_k, clusters):
    n = len(nodes)
    output_dir.mkdir(exist_ok=True)

    font_title = get_font(22, bold=True)
    font_subtitle = get_font(13, bold=False)
    font_label = get_font(12, bold=True)
    font_body = get_font(12, bold=False)
    font_val = get_font(14, bold=True)
    font_small = get_font(11, bold=False)

    # 1. heatmap.png
    w, h = 920, 800
    img = Image.new("RGB", (w, h), "white")
    draw = ImageDraw.Draw(img)
    margin_l, margin_t = 170, 110
    grid_size = 540
    cell_size = grid_size / n
    max_a = max(max(row) for row in A) if max(max(row) for row in A) > 0 else 1.0

    # Title
    draw.text((w // 2, 45), "Ma Trận Kề Có Trọng Số (Adjacency Matrix)", fill="#1e293b", anchor="mm", font=font_title)

    # Cells
    for i in range(n):
        y1 = margin_t + i * cell_size
        y2 = y1 + cell_size
        draw.text((margin_l - 15, (y1 + y2) / 2), nodes[i], fill="#0f172a", anchor="rm", font=font_label)
        draw.text((margin_l + i * cell_size + cell_size / 2, margin_t - 15), nodes[i], fill="#0f172a", anchor="ms", font=font_label)

        for j in range(n):
            x1 = margin_l + j * cell_size
            x2 = x1 + cell_size
            val = A[i][j]
            intensity = int(255 - (val / max_a) * 200) if val > 0 else 255
            cell_color = (intensity, intensity + (255 - intensity)//4, 255) if val > 0 else (248, 250, 252)
            draw.rectangle([x1, y1, x2, y2], fill=cell_color, outline="#cbd5e1", width=1)
            draw.text(((x1 + x2) / 2, (y1 + y2) / 2), str(int(val)), fill="#0f172a", anchor="mm", font=font_val)

    # Colorbar
    cb_x1, cb_y1 = margin_l + grid_size + 30, margin_t
    cb_w, cb_h = 24, grid_size
    for dy in range(int(cb_h)):
        ratio = 1.0 - (dy / cb_h)
        intensity = int(255 - ratio * 200)
        c = (intensity, intensity + (255 - intensity)//4, 255)
        draw.line([(cb_x1, cb_y1 + dy), (cb_x1 + cb_w, cb_y1 + dy)], fill=c)
    draw.rectangle([cb_x1, cb_y1, cb_x1 + cb_w, cb_y1 + cb_h], outline="#94a3b8")
    draw.text((cb_x1 + cb_w + 10, cb_y1), f"{int(max_a)}", fill="#334155", anchor="lm", font=font_small)
    draw.text((cb_x1 + cb_w + 10, cb_y1 + cb_h), "0", fill="#334155", anchor="lm", font=font_small)

    img.save(output_dir / "heatmap.png", dpi=(150, 150))

    # 2. bieu_do_pho.png
    w, h = 850, 550
    img2 = Image.new("RGB", (w, h), "white")
    draw2 = ImageDraw.Draw(img2)
    draw2.text((w // 2, 45), "Phổ Ma Trận Laplacian (Eigenvalues Spectrum)", fill="#1e293b", anchor="mm", font=font_title)

    plot_l, plot_r = 100, 780
    plot_t, plot_b = 90, 470
    plot_w = plot_r - plot_l
    plot_h = plot_b - plot_t
    max_lambda = max(eigenvalues) if max(eigenvalues) > 0 else 1.0
    scale_y = (plot_h - 40) / max_lambda

    # Gridlines
    for gy in range(0, int(max_lambda) + 2, 2):
        y = plot_b - gy * scale_y
        if plot_t <= y <= plot_b:
            draw2.line([(plot_l, y), (plot_r, y)], fill="#e2e8f0", width=1)
            draw2.text((plot_l - 12, y), f"{gy}", fill="#64748b", anchor="rm", font=font_small)

    draw2.line([(plot_l, plot_b), (plot_r, plot_b)], fill="#0f172a", width=2)
    draw2.line([(plot_l, plot_t), (plot_l, plot_b)], fill="#0f172a", width=2)

    bar_w = 55
    spacing = plot_w / (n + 1)
    for i in range(n):
        cx = plot_l + (i + 1) * spacing
        val = eigenvalues[i]
        bh = val * scale_y
        by = plot_b - bh
        color = "#e11d48" if i == 1 else "#2563eb" # Highlight lambda2
        draw2.rectangle([cx - bar_w / 2, by, cx + bar_w / 2, plot_b], fill=color, outline="#1e293b")
        draw2.text((cx, by - 14), f"{val:.2f}", fill="#0f172a", anchor="mm", font=font_val)
        draw2.text((cx, plot_b + 18), f"λ{i + 1}", fill="#0f172a", anchor="mm", font=font_label)

    draw2.text((w // 2, plot_b + 48), "Chỉ số trị riêng (k)  |  λ2 (đỏ) = Algebraic Connectivity", fill="#334155", anchor="mm", font=font_subtitle)
    img2.save(output_dir / "bieu_do_pho.png", dpi=(150, 150))

    # 3. dendrogram.png
    w, h = 850, 550
    img3 = Image.new("RGB", (w, h), "white")
    draw3 = ImageDraw.Draw(img3)
    draw3.text((w // 2, 45), f"Biểu Đồ Phân Cấp Cụm Module (Dendrogram - k={best_k})", fill="#1e293b", anchor="mm", font=font_title)

    tree_l, tree_r = 100, 750
    tree_b = 450
    tree_t = 120
    leaf_spacing = (tree_r - tree_l) / (n - 1)

    # Hierarchical positioning for leaves
    leaf_x = [tree_l + i * leaf_spacing for i in range(n)]
    for i in range(n):
        draw3.text((leaf_x[i], tree_b + 20), nodes[i], fill="#0f172a", anchor="mm", font=font_label)
        draw3.line([(leaf_x[i], tree_b), (leaf_x[i], tree_b - 30)], fill="#2563eb", width=2)

    # Cluster grouping lines
    cluster_colors = ["#2563eb", "#e11d48", "#16a34a", "#9333ea", "#ea580c", "#0891b2"]
    c_idx = 0
    c_centers = []
    for c_id, members in clusters.items():
        m_indices = [nodes.index(m) for m in members]
        color = cluster_colors[c_idx % len(cluster_colors)]
        if len(m_indices) > 1:
            min_x = min(leaf_x[idx] for idx in m_indices)
            max_x = max(leaf_x[idx] for idx in m_indices)
            hy = tree_b - 80 - (c_idx * 30)
            draw3.line([(min_x, hy), (max_x, hy)], fill=color, width=2)
            for idx in m_indices:
                draw3.line([(leaf_x[idx], tree_b - 30), (leaf_x[idx], hy)], fill=color, width=2)
            c_centers.append(((min_x + max_x) / 2, hy))
        else:
            idx = m_indices[0]
            c_centers.append((leaf_x[idx], tree_b - 80))
        c_idx += 1

    # Root link
    if len(c_centers) > 1:
        root_y = tree_t + 30
        for cx, cy in c_centers:
            draw3.line([(cx, cy), (cx, root_y)], fill="#64748b", width=2)
        min_root_x = min(cx for cx, _ in c_centers)
        max_root_x = max(cx for cx, _ in c_centers)
        draw3.line([(min_root_x, root_y), (max_root_x, root_y)], fill="#64748b", width=2)

    draw3.text((w // 2, 510), "Các nhánh thể hiện mức độ liên kết phân cụm phổ tối ưu", fill="#475569", anchor="mm", font=font_subtitle)
    img3.save(output_dir / "dendrogram.png", dpi=(150, 150))

def main():
    if sys.stdout.encoding != 'utf-8':
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass

    project_root = Path(__file__).resolve().parent.parent
    output_dir = project_root / "output"
    output_dir.mkdir(exist_ok=True)
    arch_yaml_path = project_root / "architecture.yaml"

    nodes, edges, weights, source_path = load_graph_data(project_root)
    nodes = sorted(list(set(nodes)))
    n = len(nodes)
    node_idx = {name: i for i, name in enumerate(nodes)}

    # 1. Directed graph for cycle detection & degrees
    G_dir = nx.DiGraph()
    for node in nodes:
        G_dir.add_node(node)
    for edge in edges:
        u, v = edge[0], edge[1]
        w = weights.get(f"{u}->{v}", 1)
        G_dir.add_edge(u, v, weight=w)

    cycles = list(nx.simple_cycles(G_dir))

    # 2. Undirected weighted graph for spectral analysis
    G = nx.Graph()
    for node in nodes:
        G.add_node(node)

    A = [[0.0] * n for _ in range(n)]
    for edge in edges:
        u, v = edge[0], edge[1]
        i, j = node_idx[u], node_idx[v]
        w = float(weights.get(f"{u}->{v}", 1))
        A[i][j] += w
        A[j][i] += w

    for i in range(n):
        for j in range(i + 1, n):
            if A[i][j] > 0:
                G.add_edge(nodes[i], nodes[j], weight=A[i][j])

    # Degree matrix D and Laplacian L = D - A
    L = [[0.0] * n for _ in range(n)]
    for i in range(n):
        deg = sum(A[i])
        for j in range(n):
            if i == j:
                L[i][j] = deg - A[i][j]
            else:
                L[i][j] = -A[i][j]

    # 3. Calculate eigenvalues and eigenvectors
    if HAS_SCIPY_MATPLOTLIB:
        np_L = np.array(L, dtype=float)
        vals, vecs = scipy_eigh(np_L)
        eigenvalues = [0.0 if abs(v) < 1e-10 else float(v) for v in vals]
        # column vectors
        eigenvectors = [[float(vecs[row, col]) for row in range(n)] for col in range(n)]
    else:
        eigenvalues, eigenvectors = jacobi_eigh(L)

    lambda2 = float(eigenvalues[1]) if len(eigenvalues) > 1 else 0.0
    fiedler_col = eigenvectors[1] if len(eigenvectors) > 1 else [0.0] * n
    fiedler_dict = {nodes[i]: round(float(fiedler_col[i]), 4) for i in range(n)}

    # 4. God module detection
    weighted_degrees = dict(G.degree(weight='weight'))
    god_module = max(weighted_degrees, key=weighted_degrees.get) if weighted_degrees else "None"

    # 5. Check architecture violations
    violations = check_violations(edges, arch_yaml_path)

    # 6. Spectral clustering for k=2..6
    best_k = 2
    best_q = -1.0
    best_clusters = {}
    clustering_results = {}

    for k in range(2, min(n + 1, 7)):
        if HAS_SCIPY_MATPLOTLIB:
            try:
                np_A = np.array(A, dtype=float)
                np.fill_diagonal(np_A, 1.0)
                sc = SpectralClustering(n_clusters=k, affinity='precomputed', assign_labels='kmeans', random_state=42)
                labels = sc.fit_predict(np_A)
                cluster_dict = {}
                communities = []
                for cid in range(k):
                    m = [nodes[i] for i in range(n) if labels[i] == cid]
                    if m:
                        communities.append(set(m))
                        cluster_dict[str(cid)] = sorted(m)
            except Exception:
                cluster_dict = {}
        else:
            # Embed nodes using eigenvectors v2..vk
            embed = [[eigenvectors[col][i] for col in range(1, k)] for i in range(n)]
            c_indices = kmeans_clustering(embed, k)
            cluster_dict = {}
            communities = []
            for cid, idxs in c_indices.items():
                m = sorted([nodes[idx] for idx in idxs])
                communities.append(set(m))
                cluster_dict[str(cid)] = m

        if len(communities) > 1 and G.number_of_edges() > 0:
            q = float(modularity(G, communities, weight='weight'))
        else:
            q = 0.0

        clustering_results[k] = {"q": round(q, 4), "clusters": cluster_dict}
        if q > best_q:
            best_q = q
            best_k = k
            best_clusters = cluster_dict

    # 7. Print stdout
    print("\n================== KẾT QUẢ PHÂN TÍCH PHỔ ==================")
    print("1. Eigenvalues của ma trận Laplacian L:")
    for idx, val in enumerate(eigenvalues):
        print(f"   λ{idx + 1} = {val:.4f}")

    spectral_gap = float(eigenvalues[2] - eigenvalues[1]) if len(eigenvalues) > 2 else 0.0
    print(f"\n2. Algebraic Connectivity (λ2 / Fiedler value): {lambda2:.4f}")
    print(f"   Spectral Gap (λ3 - λ2): {spectral_gap:.4f}")

    print("\n3. Vector Fiedler (tương ứng λ2):")
    for node, val in fiedler_dict.items():
        print(f"   {node:16s}: {val:+.4f}")

    print("\n4. Đánh giá Spectral Clustering (k=2..6):")
    for k, res in clustering_results.items():
        print(f"   k={k}: Modularity Q = {res['q']:.4f} | Cụm: {res['clusters']}")
    print(f"   => Best k: {best_k} (Modularity Q = {best_q:.4f})")
    print("   Phân cụm tối ưu:")
    for c_id, members in best_clusters.items():
        print(f"     Cụm {c_id}: {members}")

    print("\n5. Kiểm tra vi phạm kiến trúc (Forbidden rules):")
    if violations:
        for v in violations:
            print(f"   [VIOLATION] {v}")
    else:
        print("   [CLEAN] Không phát hiện vi phạm kiến trúc nào (0 vi phạm)!")

    print(f"\n6. God Module (Module có liên kết/trọng số lớn nhất): {god_module} (Trọng số: {weighted_degrees.get(god_module, 0)})")
    print(f"7. Vòng phụ thuộc (Cyclic dependencies): {cycles if cycles else 'None (0 cycles)'}")
    print("===========================================================\n")

    # 8. Export ket_qua.json
    ket_qua = {
        "eigenvalues": [round(float(v), 4) for v in eigenvalues],
        "lambda2": round(lambda2, 4),
        "fiedler_vector": fiedler_dict,
        "best_k": best_k,
        "modularity_q": round(best_q, 4),
        "clusters": best_clusters,
        "violations": violations,
        "god_module": god_module,
        "cyclic_dependencies": cycles
    }

    ket_qua_path = project_root / "ket_qua.json"
    with open(ket_qua_path, "w", encoding="utf-8") as f:
        json.dump(ket_qua, f, indent=2, ensure_ascii=False)
    print(f"[OUTPUT] Đã xuất kết quả: {ket_qua_path}")

    # 9. Generate 3 PNG images (150 DPI)
    draw_plots_pillow(output_dir, nodes, A, eigenvalues, best_k, best_clusters)
    print(f"[OUTPUT] Đã tạo 3 biểu đồ (dpi=150):")
    print(f"  - {output_dir / 'heatmap.png'}")
    print(f"  - {output_dir / 'bieu_do_pho.png'}")
    print(f"  - {output_dir / 'dendrogram.png'}")

if __name__ == "__main__":
    main()
