<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Waynestate\Nova\CKEditor;
use Laravel\Nova\Fields\Boolean;
use Illuminate\Support\Str;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Textarea;
use Laravel\Nova\Http\Requests\NovaRequest;

class MArticleLng extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MArticleLng::class;
    public static $displayInNavigation = false;
    // public static $globallySearchable = false;


    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'ArL_title';

    public static function label()
    {
        return 'Езикова версия секции';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'ArL_title', 'ArL_intro', 'ArL_body'
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        return [
            ID::make(__('ID'), 'ArL_id')->sortable(),
            Text::make('Заглавие', 'ArL_title')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 60);
            }),
            Textarea::make('Въведение', 'ArL_intro')->sortable(),
            Textarea::make('Ключови думи', 'ArL_meta')->sortable()->hideFromIndex(),

            CKEditor::make('Съдържание', 'ArL_body')->displayUsing(function ($text) {
                return Str::limit($text, 500);
            }),
            Boolean::make('Активен запис', 'St_id'),
            Text::make('Външен URL', 'ArL_url')->sortable()->displayUsing(function ($text) {
                return Str::limit($text, 60);
            })->hideFromIndex(),

            // Boolean::make('Активен', 'St_id'),
            BelongsTo::make('Език', 'eq_lng', SLang::class)->rules('required')->withoutTrashed(),
            Text::make('URL', 'ArL_path')->sortable()->hideFromIndex()->onlyOnForms()->hideWhenCreating(),
            BelongsTo::make('Секция', 'eq_article', MArticle::class)->rules('required')->withoutTrashed(),
            HasMany::make('Файлове', 'eq_files', MFiles::class),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
