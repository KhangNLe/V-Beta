import { render, screen } from '@testing-library/react';
import LandingPage from './page';

// Mock Next.js components to avoid issues with SSR and client-side behavior
jest.mock('next/image', () => ({
  __esModule: true,
  default: ({ alt, src, ...props }) => <img alt={alt} src={src} {...props} />,
}));

// Mock Next.js Link component to render a simple anchor tag
jest.mock('next/link', () => ({
  __esModule: true,
  default: ({ children, href, ...props }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

describe('LandingPage', () => {
  // Mock functions to track calls to IntersectionObserver methods for testing reveal-on-scroll functionality without relying on actual scrolling behavior in the test environment
  const observe = jest.fn();
  const unobserve = jest.fn();
  const disconnect = jest.fn();

  beforeEach(() => {
    observe.mockClear();
    unobserve.mockClear();
    disconnect.mockClear();

    // Mock IntersectionObserver to test reveal-on-scroll functionality without relying on actual scrolling behavior in the test environment
    global.IntersectionObserver = jest.fn((callback) => ({
      observe,
      unobserve,
      disconnect,
      callback,
    }));
  });

  // Test that the landing page renders the core content and calls to action correctly
  it('renders the core landing page content and calls to action', () => {
    render(<LandingPage />);

    expect(
      screen.getByRole('heading', { name: 'V-Beta', level: 1 }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /Discover problems, share beta, and learn from a growing community of climbers\./i,
      ),
    ).toBeInTheDocument();

    // Verify that the primary and secondary call-to-action buttons are rendered with correct links
    expect(screen.getByRole('link', { name: 'Login' })).toHaveAttribute(
      'href',
      '/login',
    );
    expect(screen.getByRole('link', { name: 'Sign Up' })).toHaveAttribute(
      'href',
      '/signup',
    );
    expect(
      screen.getByRole('link', { name: 'Browse Problems' }),
    ).toHaveAttribute('href', '/main-page');
    expect(screen.getByRole('link', { name: 'Upload Beta' })).toHaveAttribute(
      'href',
      '/signup',
    );
  });

  // Test that the landing page renders the slider images and footer attribution links correctly
  it('renders the slider images and footer attribution links', () => {
    render(<LandingPage />);

    expect(
      screen.getAllByAltText(
        /Boulderer climbing beneath an overhanging rock with crash pads below/i,
      ).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByAltText(/Two climbers on a sunlit limestone wall/i).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByAltText(/Climber topping out on a weathered stone wall/i)
        .length,
    ).toBeGreaterThan(0);

    expect(
      screen.getByRole('link', {
        name: /Instagram icons created by Freepik - Flaticon/i,
      }),
    ).toHaveAttribute('href', 'https://www.flaticon.com/free-icons/instagram');

    expect(
      screen.getByRole('link', {
        name: /Hero background photo sourced from Unsplash/i,
      }),
    ).toHaveAttribute(
      'href',
      'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b',
    );
  });

  // Test that the landing page registers intersection observers for reveal-on-scroll sections to ensure that the scroll-based animations are set up correctly
  it('registers intersection observers for reveal-on-scroll sections', () => {
    render(<LandingPage />);

    expect(global.IntersectionObserver).toHaveBeenCalled();
    expect(observe).toHaveBeenCalled();
  });
});
