import styles from '../styles/sidebar.module.css';

const menuItems = [
  { name: 'HOME', href: '/' },
  { name: 'CALCULATOR', href: '/calculator' },
];


export function Sidebar(){
return (
    <div>
      {/* Mobile Header */}
      {/* <div className={styles.mobileHeader}>
        <h1 className={styles.mobileTitle}>JEFF&apos;S WEBSITE</h1>
        <button
          className={styles.hamburger}
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle menu"
        >
          ☰
        </button>
      </div> */}

      {/* Mobile Dropdown */}
      {/* {menuOpen && (
        <ul className={styles.mobileMenu}>
          {menuItems.map((item, index) => (
            <li key={index} className={styles.mobileMenuItem}>
              <link href={item.href} className={styles.link}>
                {item.name}
              </link>
            </li>
          ))}
        </ul>
      )} */}

      {/* Desktop Sidebar */}
      <div className={styles.container}>
        <h1 className={styles.title}>JEFF&apos;S WEBSITE</h1>
        <ul className={styles.menu}>
          {menuItems.map((item, index) => (
            <li key={index} className={styles.menuItem}>
              <div className={styles.bulletContainer}>
                <span className={styles.bullet}>•</span>
                {index < menuItems.length - 1 && <div className={styles.line} />}
              </div>
              <link href={item.href} className={styles.link}>
                {item.name}
              </link>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}



