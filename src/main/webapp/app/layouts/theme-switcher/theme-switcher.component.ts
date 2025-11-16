import { Component, OnInit } from '@angular/core';
import { Theme, ThemeService } from 'app/core/theme/theme.service';
import { FaIconComponent, FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPalette } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'jhi-theme-switcher',
  templateUrl: './theme-switcher.component.html',
  styleUrls: ['./theme-switcher.component.scss'],
  imports: [FaIconComponent, FontAwesomeModule],
})
export class ThemeSwitcherComponent implements OnInit {
  themes: Theme[];
  selectedTheme: string;
  readonly faPalette = faPalette;

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.themes = this.themeService.getThemes();
    this.selectedTheme = localStorage.getItem('selected-theme') || 'yeti';
  }

  changeTheme(themeName: string): void {
    this.selectedTheme = themeName;
    this.themeService.setTheme(themeName);
  }
}
