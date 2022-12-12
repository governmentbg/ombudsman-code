<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $ArL_id
 * @property int    $Ar_id
 * @property int    $S_Lng_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $ArL_path
 * @property string $ArL_title
 * @property string $ArL_intro
 * @property string $ArL_body
 * @property string $ArL_meta
 */
class MArticleLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_article_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'ArL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Ar_id', 'S_Lng_id', 'ArL_path', 'ArL_url', 'ArL_title', 'ArL_intro', 'ArL_body', 'ArL_meta', 'St_id', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'ArL_id' => 'int', 'Ar_id' => 'int', 'S_Lng_id' => 'int', 'ArL_path' => 'string', 'ArL_url' => 'string', 'ArL_title' => 'string', 'ArL_intro' => 'string', 'ArL_body' => 'string', 'ArL_meta' => 'string', 'St_id' => 'int', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();



        static::creating(function ($article) {
            $truncated = substr($article->ArL_title, 0, 80) . "-" . $article->Ar_id;
            if ($article->S_Lng_id == 2) {
                $article->ArL_path =  Str::slug(C2L($truncated));
            }
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {

            $truncated = substr($article->ArL_title, 0, 80) . "-" . $article->Ar_id;

            $article->ArL_path = Str::slug(C2L($truncated));
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_article()
    {
        return $this->belongsTo(MArticle::class, 'Ar_id');
    }

    public function eq_files()
    {
        return $this->hasMany(MFiles::class, 'ArL_id');
    }
}
