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

class MEventLng extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MEventLng::class;
    public static $displayInNavigation = false;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'MvL_title';
    public static function label()
    {
        return 'Езикова версия';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'MvL_title',  'MvL_body'
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
            ID::make(__('ID'), 'MvL_id')->sortable(),
            Text::make('Заглавие', 'MvL_title')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 60);
            }),

            CKEditor::make('Съдържание', 'MvL_body')->displayUsing(function ($text) {
                return Str::limit($text, 500);
            }),

            Boolean::make('Активен', 'St_id'),
            BelongsTo::make('Език', 'eq_lng', SLang::class)->rules('required')->withoutTrashed(),
            BelongsTo::make('Събитие', 'eq_event', MEvent::class)->rules('required')->withoutTrashed(),
            HasMany::make('Файлове', 'eq_files_event', MFiles2::class),
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
