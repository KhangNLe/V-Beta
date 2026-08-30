import { render, screen } from '@testing-library/react';
import AboutPage from './page';

jest.mock('next/image', () => ({
  __esModule: true,
  default: ({ alt, src, ...props }) => <img alt={alt} src={src} {...props} />,
}));

jest.mock('next/link', () => ({
  __esModule: true,
  default: ({ children, href, ...props }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe('AboutPage', () => {
  it('renders V-Beta about content and navigation', () => {
    render(<AboutPage />);

    expect(
      screen.getByRole('heading', { name: 'V-Beta', level: 1 }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/keep that knowledge tied to a wall even after it is reset/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Why it exists' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Built for the Co-op' })).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Minnesota Climbing Cooperative' }),
    ).toHaveAttribute('href', 'https://www.mnclimbingcoop.com/');
    expect(screen.getByRole('link', { name: 'Browse Problems' })).toHaveAttribute(
      'href',
      '/main-page',
    );
    expect(screen.getByRole('link', { name: 'Back to Home' })).toHaveAttribute(
      'href',
      '/',
    );
    expect(screen.getByRole('heading', { name: 'Contributors' })).toBeInTheDocument();
    expect(
      screen.getByText(/personal project, currently maintained only by Khang/i),
    ).toBeInTheDocument();
    const githubLinks = screen.getAllByRole('link', { name: 'GitHub' });
    expect(githubLinks.length).toBeGreaterThan(0);
    githubLinks.forEach((link) => {
      expect(link).toHaveAttribute('href', 'https://github.com/KhangNLe/V-Beta');
    });
  });
});
