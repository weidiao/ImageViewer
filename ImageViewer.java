
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ImageViewer extends JFrame {
	class ImagePanel extends JPanel {
		Point last = null;

		ImagePanel() {
			addMouseListener(new MouseAdapter() {
				/**
				 * @three mode
				 * @scale 1 left mouse button
				 * @move 2 right mouse button
				 * @browse 0 left mouse button double click
				 */
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 1) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							mode = 1;
						} else if (e.getButton() == MouseEvent.BUTTON3) {
							mode = 2;
						}
					} else if (e.getClickCount() == 2.0) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							mode = 0;
							initRect();
							repaint();
						}
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					last = e.getPoint();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
			addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					switch (mode) {
					case 0:// next image,prev image
						fileIndex = (fileIndex + e.getWheelRotation() + res.size()) % res.size();
						read();
						initRect();
						repaint();
						break;
					case 1:// change image size
						resizeImg(1 - e.getWheelRotation() * 0.1, e.getPoint());
						repaint();
						break;
					case 2:// change image position
						moveImg(e.getWheelRotation() * 40);
						repaint();
						break;
					default:
						try {
							throw new Exception("wrong mode");
						} catch (Exception e1) {
							e1.printStackTrace();
							System.exit(-1);
						}
					}
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {

				@Override
				public void mouseDragged(MouseEvent e) {
					if (last == null)
						return;
					moveImg(last.x - e.getX(), last.y - e.getY());
					repaint();
					last = e.getPoint();
				}
			});
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						Path file = res.get(fileIndex);
						if (JOptionPane.showConfirmDialog(ImageViewer.this, "确定删除\"" + file.getFileName() + "\"吗?", "删除",
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							try {
								Files.delete(file);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							removeFromList();
						}
					} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_LEFT) {
						fileIndex = (fileIndex - 1 + res.size()) % res.size();
						read();
						initRect();
						repaint();
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_RIGHT) {
						fileIndex = (fileIndex + 1 + res.size()) % res.size();
						read();
						initRect();
						repaint();
					}
				}
			});
		}

		@Override
		public void paint(Graphics g) {
			if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight())
				buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			Graphics gg = buffer.getGraphics();
			gg.setColor(Color.BLACK);
			gg.fillRect(0, 0, getWidth(), getHeight());
			double w = Math.min(rec.x * 2.0, getWidth());
			double h = Math.min(rec.y * 2.0, getHeight());
			double x = rec.x - w / 2.0;
			double y = rec.y - h / 2.0;
			Rectangle2D.Double src = new Rectangle2D.Double(x / rec.width * img.getWidth(),
					y / rec.height * img.getHeight(), w / rec.width * img.getWidth(), h / rec.height * img.getHeight());
			Rectangle2D.Double des = new Rectangle2D.Double((getWidth() - w) / 2.0, (getHeight() - h) / 2.0, w, h);
			gg.drawImage(img.getSubimage((int) src.x, (int) src.y, (int) src.width, (int) src.height).getScaledInstance(
					(int) des.width, (int) des.height, Image.SCALE_SMOOTH), (int) des.x, (int) des.y, null);
			g.drawImage(buffer, 0, 0, null);
		}
	}

	public static void main(String[] args) throws IOException {
		Path path = null;
		if (args.length == 0) {
			path = Paths.get(System.getProperty("user.home"), "Pictures");
		} else {
			path = Paths.get(args[0]);
		}
		new ImageViewer(path);
	}

	// the image file list
	ArrayList<Path> res = new ArrayList<>();
	int fileIndex = 0;
	// avoid bling
	BufferedImage buffer = null;
	// the current image
	BufferedImage img;
	// the main data,the most important data.
	// rec.x and rec.y means the point of the center of the screen
	// rec.w and rec.h means the width and the height of the current image
	Rectangle2D.Double rec = new Rectangle2D.Double();
	int mode = 0;
	ImagePanel imagePanel = new ImagePanel();

	public ImageViewer(Path path) throws IOException {
		load(path);
		read();
		initRect();
		setTitle("image viewer");
		setExtendedState(MAXIMIZED_BOTH);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		add(imagePanel);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setVisible(true);
		imagePanel.requestFocus(false);
	}

	void load(Path path) {
		try {
			Path parent = Files.isDirectory(path) ? path : path.getParent();
			Files.list(parent).forEach(p -> {
				if (Files.isRegularFile(p)) {
					res.add(p);
					if (path.getFileName().equals(p.getFileName())) {
						fileIndex = res.size() - 1;
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (res.size() == 0) {
			JOptionPane.showConfirmDialog(this, "no image found", "error", JOptionPane.YES_OPTION);
			System.exit(0);
		}
	}

	void read() {
		try {
			img = ImageIO.read(res.get(fileIndex).toFile());
			if (img == null) {
				removeFromList();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void removeFromList() {
		res.remove(fileIndex);
		if (res.size() == 0) {
			System.exit(0);
		}
		if (fileIndex == res.size()) {
			fileIndex = 0;
		}
		read();
		initRect();
		repaint();
	}

	boolean isTooBig() {
		double w = Math.min(rec.width, imagePanel.getWidth()), h = Math.min(rec.height, imagePanel.getHeight());
		return w / rec.width * img.getWidth() < 1 || h / rec.height * img.getHeight() < 1;
	}

	void resizeImg(double delta, Point p) {
		Rectangle2D.Double old = new Rectangle2D.Double(rec.x, rec.y, rec.width, rec.height);
		rec.width *= delta;
		rec.height = (double) img.getHeight() / img.getWidth() * rec.width;
		if (rec.width < 2.0 || rec.height < 2.0 || isTooBig()) {
			rec = old;
			return;
		}
		double w = Math.min(old.x * 2.0, imagePanel.getWidth()), h = Math.min(old.y * 2.0, imagePanel.getHeight());
		Point center = new Point(imagePanel.getWidth() / 2, imagePanel.getHeight() / 2);
		if (Math.abs(center.y - p.y) > h / 2.0) {
			p.y = (int) center.y;
		}
		if (Math.abs(center.x - p.x) > w / 2.0) {
			p.x = (int) center.x;
		}

		// 鼠标点在old视窗中对应的点
		double x = old.x + p.x - center.x, y = old.y + p.y - center.y;
		// 鼠标点击点在新视窗中对应的点
		double xx = x / old.width * rec.width, yy = y / old.height * rec.height;
		// 新视窗居中显示
		rec.x = rec.width / 2.0;
		rec.y = rec.height / 2.0;
		// 下面的移动不一定真的会发生
		moveImg(xx - rec.x - (p.x - center.x), yy - rec.y - (p.y - center.y));
	}

	void moveImg(double dx, double dy) {
		moveImgHorizontally(dx);
		moveImgVertically(dy);
	}

	void moveImg(int delta) {
		int state = getNowState();
		if (state == 3)
			state = img.getWidth() > img.getHeight() ? 2 : 1;
		if (state == 1) {
			moveImgVertically(delta);
		} else if (state == 2.0) {
			moveImgHorizontally(delta);
		}
	}

	void moveImgHorizontally(double delta) {
		if (rec.width > imagePanel.getWidth()) {
			rec.x += delta;
			rec.x = Math.min(rec.width - imagePanel.getWidth() / 2.0, rec.x);
			rec.x = Math.max(imagePanel.getWidth() / 2.0, rec.x);
		}
	}

	void moveImgVertically(double delta) {
		if (rec.height > imagePanel.getHeight()) {
			rec.y += delta;
			rec.y = Math.min(rec.height - imagePanel.getHeight() / 2.0, rec.y);
			rec.y = Math.max(imagePanel.getHeight() / 2.0, rec.y);
		}
	}

	/**
	 * @put the image at the center of the panel
	 * @init the rect
	 */
	void initRect() {
		rec.width = img.getWidth();
		rec.height = img.getHeight();
		rec.x = rec.width / 2.0;
		rec.y = rec.height / 2.0;
	}

	/**
	 * @four states @00:too small @01:too long @10:too wide @11:too big
	 */
	int getNowState() {
		int x = rec.width < imagePanel.getWidth() ? 0 : 1, y = rec.height < imagePanel.getHeight() ? 0 : 1;
		return x << 1 | y;
	}
}
